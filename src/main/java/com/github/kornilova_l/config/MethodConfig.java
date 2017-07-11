package com.github.kornilova_l.config;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.kornilova_l.config.ConfigStorage.Config.getParametersList;

@SuppressWarnings("PublicField")
public class MethodConfig implements Comparable<MethodConfig> {
    private final static Pattern paramsPattern = Pattern.compile("(\\[?)(C|Z|S|I|J|F|D|B|(:?L[^;]+;))");
    public String methodPattern;
    public String classPattern;
    public LinkedList<Parameter> parameters = new LinkedList<>();
    public boolean isEnabled = true;
    public boolean saveReturnValue = false;

    @SuppressWarnings("unused")
    public MethodConfig() {
    }

    public MethodConfig(@NotNull PsiMethod psiMethod) {
        setNames(psiMethod);
        System.out.println(this);
    }

    public MethodConfig(String methodConfigLine) {
        setNames(methodConfigLine);
    }

    public MethodConfig(String classPattern, String methodPattern, String parametersPattern) {
        this.classPattern = classPattern;
        this.methodPattern = methodPattern;
        if (parametersPattern.charAt(parametersPattern.length() - 1) == '+') {
            saveReturnValue = true;
        }
        parametersPattern = parametersPattern.substring(1, parametersPattern.lastIndexOf(")"));
        String[] stringParameters = parametersPattern.split(" *, *");
        for (String stringParameter : stringParameters) {
            parameters.addLast(new Parameter(stringParameter, ""));
        }
    }

    static String parametersToString(List<MethodConfig.Parameter> parameters) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("(");
        for (int i = 0; i < parameters.size(); i++) {
            MethodConfig.Parameter parameter = parameters.get(i);
            stringBuilder.append(parameter.type).append(" ").append(parameter.name);
            if (i != parameters.size() - 1) {
                stringBuilder.append(", ");
            }
        }
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    public static String parametersToStringForJvm(List<Parameter> parameters) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("(");
        for (Parameter parameter : parameters) {
            stringBuilder.append(parameter.jvmType);
        }
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    @Nullable
    public static String[] getParamsDesc(String partOfDescWithParams) {
        ArrayList<String> paramsDesc = new ArrayList<>();
        Matcher m = paramsPattern.matcher(partOfDescWithParams);
        while (m.find()) {
            paramsDesc.add(m.group());
        }
        if (paramsDesc.isEmpty()) {
            return null;
        }
        String[] ret = new String[paramsDesc.size()];
        paramsDesc.toArray(ret);
        return ret;
    }

    private void setNames(String methodConfigLine) {
        String packageAndClass = methodConfigLine.substring(0, methodConfigLine.indexOf("."));
        int slashPos = packageAndClass.lastIndexOf("/");
        if (slashPos != -1) {
            packageName = packageAndClass.substring(0, packageAndClass.lastIndexOf("/"));
        } else {
            packageName = null;
        }
        classPattern = packageAndClass.substring(
                packageAndClass.lastIndexOf("/") + 1, packageAndClass.length());
        methodPattern = methodConfigLine.substring(methodConfigLine.indexOf(".") + 1, methodConfigLine.indexOf("("));
        String[] jvmTypes = getParamsDesc(
                methodConfigLine.substring(methodConfigLine.indexOf("("), methodConfigLine.indexOf(" "))
        );
        if (jvmTypes == null) {
            return;
        }
        for (String jvmType : jvmTypes) {
            parameters.add(new Parameter(jvmType));
        }
        setParametersEnabled(parameters, methodConfigLine.substring(methodConfigLine.indexOf(" ") + 1, methodConfigLine.length()));
    }

    private static void setParametersEnabled(List<Parameter> parameters, String isEnabledString) {
        assert parameters.size() == isEnabledString.length();
        for (int i = 0; i < isEnabledString.length(); i++) {
            parameters.get(i).isEnable = isEnabledString.charAt(i) == 'y';
        }
    }

    @Override
    public String toString() {
        return getQualifiedName() + parametersToString(parameters);
    }

    public String getQualifiedName() {
        if (packageName == null) {
            return classPattern + "." + methodPattern;
        }
        return packageName + "." + classPattern + "." + methodPattern;
    }

    public String getQualifiedNameWithSlashes() {
        if (packageName == null) {
            return classPattern + "." + methodPattern;
        }
        return packageName.replace(".", "/") + "/" + classPattern + "." + methodPattern;
    }

    @Override
    public int compareTo(@NotNull MethodConfig o) {
        return toString().compareTo(o.toString());
    }

    private void setNames(PsiMethod psiMethod) {
        classPattern = null;
        methodPattern = psiMethod.getName();
        parameters = getParametersList(psiMethod.getParameterList().getParameters());
        PsiClass psiClass = psiMethod.getContainingClass();
        assert psiClass != null;

        while (psiClass != null) {
            classPattern = classPattern == null ?
                    psiClass.getName() :
                    psiClass.getName() + "." + classPattern;
            psiClass = psiClass.getContainingClass();
        }
        String fullName = psiMethod.getContainingClass().getQualifiedName();
        assert fullName != null;
        int beginningOfClassName = fullName.indexOf(classPattern);
        if (beginningOfClassName != 0) {
            packageName = fullName.substring(0, beginningOfClassName - 1);
        } else {
            packageName = null;
        }
    }

    public String toStringForJvm() {
        return getQualifiedNameWithSlashes() + parametersToStringForJvm(parameters);
    }

    public String getWhichParamsAreEnabled() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Parameter parameter : parameters) {
            stringBuilder.append(parameter.isEnable ? "y" : "n");
        }
        return stringBuilder.toString();
    }

    public Object getJvmClassName() {
        return classPattern.replaceAll("\\.", "/");
    }

    public static class Parameter {
        public String type;
        public String name;
        public boolean isEnable = false;

        @SuppressWarnings("unused")
        Parameter() {
        }

        Parameter(@NotNull String type, @NotNull String name) {
            this.type = type;
            this.name = name;
        }
    }
}
