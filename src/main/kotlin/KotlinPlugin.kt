package at.redsource.play.kotlin

import play.Logger
import play.Play
import play.PlayPlugin
import play.classloading.ApplicationClasses
import play.vfs.VirtualFile
import java.io.File

/**
 * The main plugin, doing compilation and so on.
 */
class KotlinPlugin : PlayPlugin() {

    /**
     * Name of the application, used to find the class directory to which .
     */
    private val appName get() = Play.configuration.getProperty("application.name", "application")

    /**
     * The directory to which the IDE compiles to.
     *
     * These classes are loaded by the plugin.
     */
    private val classDir get() = File("${Play.tmpDir.absolutePath}/classes/production/${appName}/")

    override fun onApplicationStart() = Logger.info("Kotlin Plugin is active")

    override fun compileSources(): Boolean {
        if (!classDir.exists()) {
            Logger.warn(
                    "$classDir does not exist. Kotlin support is therefore disabled. " +
                    "Make sure that your IDE compiles to this folder. " +
                    "The last part of the directory name is the application name. " +
                    "You can modify it by changing `application.name` in your application.conf file."
            )

            return false
        }

        Logger.info("Loading compiled Kotlin and Java Classes from $classDir")

        classDir.walkTopDown()
            // only classes
            .filter { it.name.endsWith(".class") }
            // Plugins that are delivered with the app must be excluded because
            // they are compiled and loaded by the play framework before any application class.
            // Having them defined twice leads to strange effects.
            // As a consequence we don't support writing application-delivered plugins in kotlin.
            .filter { !it.name.endsWith("Plugin.class") }
            // add every class to the play classes as a compiled class
            .forEach { classFile ->
                if (Logger.isTraceEnabled()) {
                    Logger.trace("""adding class file "$classFile" as class name "${toClassName(classFile)}"""")
                }

                // TODO: Kotlin classes should have the enhance() method disabled
                Play.classes.add(ApplicationClasses.ApplicationClass().apply {
                    name = toClassName(classFile)
                    // TODO: Here we should have the .kt source file but we don't know which one it is based on the class file.
                    // Can we find out the source file? Do we have problems because of this workaround?
                    javaFile = VirtualFile.open(classFile)
                    refresh()
                    compiled(classFile.readBytes())
                })
            }

        return true
    }

    private fun toClassName(file: File) = file.absolutePath
        .removePrefix(classDir.path)
        .removePrefix("/")
        .removeSuffix(".class")
        .replace('/', '.')
}