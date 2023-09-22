# Hermit IntelliJ plugin

This IntelliJ plugin provides configuration from the project's Hermit environment to the IDE. 
It is supported both in Idea and GoLand.

## Features

This plugin loads environment variables automatically from a Hermit environment if one exists at the root of the project.
The environment variables will be available in the terminal and executions.

If there is a JDK or GO environment in the Hermit environment, the user is shown a popup allowing them to use the SDK.
If a Hermit managed SDK is used, it is automatically upgraded when upgraded in hermit.

All Hermit changes are automatically reflected in the IDE.

## Building

To build the plugin, run

    gradle buildPlugin

This generates a zip file at

    build/distributions/idea-plugin-1.0-SNAPSHOT.zip

## Install

To install the package, go to the plugin preferences in your IDE, and select "Install plugin from disk"
If you then select the zip file generated above and restart your IDE, the plugin should be ready.

## Developing locally

You can use the `runIde` task to start an IDE with the plugin installed. You can also run that in debug mode.

---

Copyright 2021 Square, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
