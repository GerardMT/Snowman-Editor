name := "Snowman Editor"

version := "1.1.1"

scalaVersion := "2.12.7"

libraryDependencies += "org.scala-lang" % "scala-swing" % "2.10+"
libraryDependencies += "org.scala-lang" % "scala-library" % scalaVersion.value

Compile /  mainClass := Some("gmt.terminal.Terminal")

Compile / packageBin / artifactPath := baseDirectory.value / "out" / "snowman_editor.jar"

assemblyOutputPath in assembly := file("out/snowman_editor.jar")