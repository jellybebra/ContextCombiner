# Context Combiner

A plugin for JetBrains IDEs to merge together selected files and copy the result to clipboard for easy pasting into LLM.

## Tasks

- edit [plugin.xml][file:plugin.xml]

  You can read more about this file in the [Plugin Configuration File][docs:plugin.xml] section of our documentation.
  If you're still not quite sure what this is all about, read our introduction: [What is the IntelliJ Platform?][docs:intro]

## Publishing

Releasing the plugin to [JetBrains Marketplace](https://plugins.jetbrains.com) can be done directly from the command line using the `publishPlugin` Gradle task.

### 1. Setup JetBrains Marketplace Token
1. Go to your profile settings on the JetBrains Marketplace under **My Tokens** (or visit [plugins.jetbrains.com/author/me/tokens](https://plugins.jetbrains.com/author/me/tokens)).
2. Generate a new **Personal Access Token**.
3. Create (or open) a global `gradle.properties` file in your home directory:
   - **Windows**: `C:\Users\<Username>\.gradle\gradle.properties`
   - **macOS / Linux**: `~/.gradle/gradle.properties`
4. Add the following line to the file:
   ```properties
   intellijPublishToken=perm:your_token_here
   ```

### 2. Publish the Plugin
Run the following command in the project root:
- **Windows (PowerShell)**: `.\gradlew.bat publishPlugin`
- **Windows (cmd)**: `gradlew.bat publishPlugin`
- **macOS / Linux**: `./gradlew publishPlugin`

[docs]: https://plugins.jetbrains.com/docs/intellij

[docs:intro]: https://plugins.jetbrains.com/docs/intellij/intellij-platform.html?from=IJPluginTemplate

[docs:plugin.xml]: https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html?from=IJPluginTemplate

[docs:publishing]: https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate

[file:plugin.xml]: ./src/main/resources/META-INF/plugin.xml

[gh:code-samples]: https://github.com/JetBrains/intellij-sdk-code-samples

[gh:intellij-platform-gradle-plugin]: https://github.com/JetBrains/intellij-platform-gradle-plugin

[gh:intellij-platform-gradle-plugin-docs]: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html

[gh:intellij-platform-gradle-plugin-runIde]: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-tasks.html#runIde

[gh:intellij-platform-gradle-plugin-verifyPlugin]: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-tasks.html#verifyPlugin

[gradle:lifecycle-tasks]: https://docs.gradle.org/current/userguide/java_plugin.html#lifecycle_tasks

[jb:github]: https://github.com/JetBrains/.github/blob/main/profile/README.md

[jb:forum]: https://platform.jetbrains.com/

[jb:quality-guidelines]: https://plugins.jetbrains.com/docs/marketplace/quality-guidelines.html

[jb:paid-plugins]: https://plugins.jetbrains.com/docs/marketplace/paid-plugins-marketplace.html

[jb:quality-guidelines]: https://plugins.jetbrains.com/docs/marketplace/quality-guidelines.html

[jb:ipe]: https://jb.gg/ipe

[jb:ui-guidelines]: https://jetbrains.github.io/ui
