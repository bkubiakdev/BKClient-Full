package dev.bkclient.systems.file.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface FileInfo {
   String name();

   String fileType() default dev.bkclient.systems.file.FileManager.DEFAULT_FILE_TYPE;
}
