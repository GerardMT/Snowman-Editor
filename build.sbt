name := "Snowman Editor"

version := "1.1.1"

scalaVersion := "2.12.7"

libraryDependencies += "org.scala-lang" % "scala-swing" % "2.10+"

Compile /  mainClass := Some("gmt.terminal.Terminal")

Compile / packageBin / artifactPath := baseDirectory.value / "out" / "snowman_editor.jar"