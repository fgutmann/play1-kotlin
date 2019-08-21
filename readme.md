# Play Framework 1 - Kotlin Plugin

This plugin is for the old, play framework 1 and has nothing to do with play framework 2!

Everything was tested with play framework version `1.5.0`.
It is known **not to work with version `1.4.x`** because of changes in the framework (remove of CorePlugin, etc.).
If there is demand for older versions please let us know in the issues.

## Goals

The main goal of this plugin is to provide kotlin support for play framework 1 projects.
The main intention of it is to ease the transition from play framework 1 projects to kotlin based frameworks.

* It should be possible to use kotlin code alongside java throughout most parts of the project.
* It should be possible to support `kotlinx-html` based templates.
  Optimally groovy templates could call `kotlinx-html`templates and the other way around to be able
  to migrate templates one by one.
  
### Non-Goals
  
* It is not a goal to support writing play plugins in kotlin.
  The plugins must compile without any dependency on a kotlin class.
  As a work around one can call kotlin logic from plugins via reflection.
* It is a non-goal to support bytecode enhancers provided by the play framework for kotlin code.
    * `PropertiesEnhancer`: Unnecessary for kotlin classes. There is full property support in kotlin.
    * `ControllerEnhancer`: Controllers are the most deeply integrated part with play framework.
      Since you will have to re-write almost all controller code for the new framework anyways,
      there is not much sense in re-writing controllers first in kotlin for play framework.
    * `ContinuationEnhancer`: You can use coroutines in kotlin :-)
    * `LocalVariableNamesEnhancer`: Should only be used in controller code. See section about `ControllerEnhancer` above.
    * `MailerEnhancer`: Not sure what it exactly does but only used for sub-classes of `play.mvc.Mailer`.
    * `SigEnhancer`: Used for change detection.
      TODO: define how to deal with this when we now how to work with compiling and code change detection.
      Maybe we have to support it.
   
## How it works

Compilation is completely disabled from the play framework.
This means that you have to compile all code (java and kotlin) with an external tool.

IntelliJ IDEA does a great job with this. With automatic compilation turned on the workflow is pretty smooth.

The play framework will pick up changed classes. Java classes are enhanced the default play way.
This plugin makes sure to not enhance kotlin classes.

## How to use it

1. Add this module as a dependency.
1. Add the line `0:at.redsource.play.kotlin.KotlinPlugin` at the top of your `conf/play.plugins` file to enable the kotlin plugin with highest priority.
   If you don't have that file yet just create it.
1. Set up IntelliJ to take take the compilation responsibility.
   By default the play framework holds a bytecode cache in `/tmp/classes/`. There it puts the enhanced bytecode.
   When you set up IntelliJ using `play idealize` it is set up to compile to `/tmp/classes/production/project-name/` to not conflict with play.
1. Turn off the bytecode cache (probably not necessary) by adding `play.bytecodeCache=false` to `application.conf`
1. Make sure that IntelliJ compiles with method parameter names (java 8). Add `-parameters` to the javac compiler args.
1. Create kotlin classes in your codebase.
1. Start to be more happy and have more and better growing hair thanks to kotlin :-)

## TODO

Stop enhancing of Kotlin classes.

Play Dev Mode and reload is not supported at all (only works for now after an IntelliJ compile and then a fresh play start).

How to compile for production on a CI server?
We might need gradle and could then possibly use it for development as well.

## Play Internals

Whenever a class is loaded by name it will be compiled by the internal play compiler when it is not yet defined before.
This happens even when the `compileSources` method of a plugin returns `true`.

### Compiler Flow

```
ApplicationClassLoader -> getAllClasses() -> PluginCollection.compileSources()
foreach ApplicationClassLoader -> 
    loadApplicationClass(String name)
    if ApplicationClass.javaByteCode == null then compile()
    ApplicationClass.enhance()
        ApplicationClass -> enhance() -> PluginCollection.enhance()
```