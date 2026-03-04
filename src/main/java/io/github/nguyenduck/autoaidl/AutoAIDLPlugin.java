package io.github.nguyenduck.autoaidl;

import com.android.build.api.variant.AndroidComponentsExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.io.File;
import java.util.Objects;

public class AutoAIDLPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {

        project.getPlugins().withId("com.android.application", plugin ->
                configureAndroid(project));

        project.getPlugins().withId("com.android.library", plugin ->
                configureAndroid(project));
    }

    private void configureAndroid(Project project) {

        AndroidComponentsExtension<?, ?, ?> androidComponents =
                project.getExtensions()
                        .getByType(AndroidComponentsExtension.class);

        androidComponents.onVariants(androidComponents.selector().all(), variant -> {

            String taskName = "generate" + capitalize(variant.getName()) + "AutoAidl";

            File generatedDir = new File(
                    project.getBuildFile(),
                    "generated/aidl/" + variant.getName()
            );

            org.gradle.api.tasks.TaskProvider<GenerateAidlTask> taskProvider =
                    project.getTasks().register(taskName, GenerateAidlTask.class, task -> {
                        task.setGroup("build");
                        task.getSourceDir().set(new File(project.getProjectDir(), "src/main/java"));
                        task.getOutputDir().set(generatedDir);
                    });

            // Inject AIDL source folder
            Objects.requireNonNull(variant.getSources()
                            .getAidl())
                    .addGeneratedSourceDirectory(
                            taskProvider,
                            GenerateAidlTask::getOutputDir
                    );
        });
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}