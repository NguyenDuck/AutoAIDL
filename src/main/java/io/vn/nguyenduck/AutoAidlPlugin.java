package io.vn.nguyenduck;

import org.gradle.api.*;
import org.gradle.api.tasks.*;

public class AutoAidlPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {

        TaskProvider<GenerateAidlTask> task =
                project.getTasks().register(
                        "generateAutoAidl",
                        GenerateAidlTask.class,
                        t -> {
                            t.setGroup("auto-aidl");
                        });

        project.afterEvaluate(p -> {
            Task compileAidl =
                    p.getTasks().findByName("compileDebugAidl");

            if (compileAidl != null) {
                compileAidl.dependsOn(task);
            }
        });
    }
}