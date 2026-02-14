addDependencyTreePlugin
addSbtPlugin("com.github.sbt"  % "sbt-git"             % "2.0.1")
addSbtPlugin("com.eed3si9n"    % "sbt-buildinfo"       % "0.11.0")
addSbtPlugin("com.github.sbt"  % "sbt-native-packager" % "1.9.16")
addSbtPlugin("ch.epfl.scala"   % "sbt-scalafix"        % "0.14.5")
addSbtPlugin("org.scalameta"   % "sbt-scalafmt"        % "2.5.2")
addSbtPlugin("com.thesamet"    % "sbt-protoc"          % "1.0.7")
addSbtPlugin("org.scalameta"   % "sbt-native-image"    % "0.3.4")
addSbtPlugin("com.github.sbt"  % "sbt-dynver"          % "5.0.1")
addSbtPlugin("org.wartremover" % "sbt-wartremover"     % "3.5.5")
addSbtPlugin("org.wartremover" % "sbt-wartremover-contrib" % "2.4.3", "1.0", "2.12")

libraryDependencies +=
  "com.thesamet.scalapb.zio-grpc" %% "zio-grpc-codegen" % "0.6.1"