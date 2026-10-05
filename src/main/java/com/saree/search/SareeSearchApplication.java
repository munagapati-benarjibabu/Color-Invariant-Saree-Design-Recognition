package com.saree.search;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.global.opencv_core;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SareeSearchApplication {
  public static void main(String[] args) {
    Loader.load(opencv_core.class); // Bundled JavaCPP native library, including Apple Silicon.
    SpringApplication.run(SareeSearchApplication.class, args);
  }
}
