package com.github.kornilova_l.server;

import com.github.kornilova_l.profiler.ProfilerFileManager;
import com.github.kornilova_l.protos.TreeProtos;
import com.github.kornilova_l.protos.TreesProtos;
import com.github.kornilova_l.server.trees.TreeBuilder;
import com.intellij.openapi.util.io.BufferExposingByteArrayOutputStream;
import com.intellij.openapi.util.io.StreamUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.RestService;
import org.jetbrains.io.Responses;

import java.io.*;
import java.lang.reflect.InvocationTargetException;

public class ProfilerRestService extends RestService {

    private static final com.intellij.openapi.diagnostic.Logger LOG =
            com.intellij.openapi.diagnostic.Logger.getInstance(ProfilerRestService.class);
    private final TreeBuilder treeBuilder = new TreeBuilder();

    @NotNull
    @Override
    protected String getServiceName() {
        return ServerNames.NAME;
    }

    @Override
    protected boolean isMethodSupported(@NotNull HttpMethod method) {
        return method == HttpMethod.GET;
    }

    @Override
    protected boolean isPrefixlessAllowed() {
        return true;
    }

    @Override
    protected boolean isHostTrusted(@NotNull FullHttpRequest request) throws InterruptedException, InvocationTargetException {
        return true;
    }

    @Nullable
    @Override
    public String execute(@NotNull QueryStringDecoder urlDecoder,
                          @NotNull FullHttpRequest request,
                          @NotNull ChannelHandlerContext context) throws IOException {
        String uri = urlDecoder.path(); // without parameters
        LOG.info("Lucinda. Request: " + uri);
        switch (uri) {
            case ServerNames.CALL_TREE:
                LOG.info("call-tree.html");
                sendStatic(request, context, ServerNames.MAIN_NAME + "/call-tree.html", "text/html");
                break;
            case ServerNames.OUTGOING_CALLS:
                LOG.info("outgoing-calls.html");
                sendStatic(request, context, ServerNames.MAIN_NAME + "/outgoing-calls.html", "text/html");
                break;
            case ServerNames.CALL_TREE_JS_REQUEST:
                LOG.info("CALL_TREE_JS_REQUEST");
                sendTrees(request, context, treeBuilder.getCallTree());
                break;
            case ServerNames.OUTGOING_CALLS_JS_REQUEST:
                LOG.info("OUTGOING_CALLS_JS_REQUEST");
                sendTree(request, context, treeBuilder.getOutgoingCalls());
                break;
            default:
                if (ServerNames.CSS_PATTERN.matcher(uri).matches()) {
                    LOG.info("CSS");
                    sendStatic(request, context, uri, "text/css");
                } else if (ServerNames.JS_PATTERN.matcher(uri).matches()) {
                    LOG.info("JS");
                    sendStatic(request, context, uri, "text/javascript");
                } else if (ServerNames.FONT_PATTERN.matcher(uri).matches()) {
                    sendStatic(request, context, uri, "application/octet-stream");
                } else {
                    return "Not Found";
                }
        }
        return null;
    }

    private static void sendTrees(FullHttpRequest request,
                                  ChannelHandlerContext context,
                                  TreesProtos.Trees trees) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            trees.writeTo(outputStream);
            sendBytes(request, context, "application/octet-stream", outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendTree(FullHttpRequest request,
                                 ChannelHandlerContext context,
                                 TreeProtos.Tree tree) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            tree.writeTo(outputStream);
            sendBytes(request, context, "application/octet-stream", outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendStatic(FullHttpRequest request,
                                   ChannelHandlerContext context,
                                   String fileName,
                                   String contentType) throws IOException {
        LOG.info("Lucinda. Got filename: " + fileName);
        String filePath = fileName.replaceFirst("/[^/]+/", ProfilerFileManager.getStaticDir().getAbsolutePath() + "/");
        LOG.info("Lucinda. I will send this file: " + filePath);
        try (
                BufferExposingByteArrayOutputStream byteOut = new BufferExposingByteArrayOutputStream();
                InputStream stream = new FileInputStream(
                        new File(filePath)
                )
        ) {
            byteOut.write(StreamUtil.loadFromStream(stream));
            sendBytes(request, context, contentType, byteOut.getInternalBuffer());
        }
    }

    private static void sendBytes(FullHttpRequest request,
                                  ChannelHandlerContext context,
                                  String contentType,
                                  byte[] bytes) {
        HttpResponse response = Responses.response(
                contentType,
                Unpooled.wrappedBuffer(bytes)
        );
        Responses.addNoCache(response);
        Responses.send(response, context.channel(), request);
    }
}
