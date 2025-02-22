package xyz.jpenilla.resourcefactory.velocity

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import xyz.jpenilla.resourcefactory.ConfigurateSingleFileResourceFactory
import xyz.jpenilla.resourcefactory.ResourceFactory
import xyz.jpenilla.resourcefactory.util.Pattern
import xyz.jpenilla.resourcefactory.util.ProjectMetaConventions
import xyz.jpenilla.resourcefactory.util.getValidating
import xyz.jpenilla.resourcefactory.util.nullAction
import xyz.jpenilla.resourcefactory.util.nullIfEmpty
import xyz.jpenilla.resourcefactory.util.validate

fun Project.velocityPluginJson(configure: Action<VelocityPluginJson> = nullAction()): VelocityPluginJson {
    val yml = VelocityPluginJson(objects)
    yml.setConventionsFromProjectMeta(this)
    configure.execute(yml)
    return yml
}

class VelocityPluginJson constructor(
    @Transient
    private val objects: ObjectFactory
) : ConfigurateSingleFileResourceFactory.ObjectMapper.ValueProvider, ProjectMetaConventions, ResourceFactory.Provider {

    companion object {
        private const val PLUGIN_ID_PATTERN: String = "[a-z][a-z0-9-_]{0,63}"
    }

    @Pattern(PLUGIN_ID_PATTERN, "Velocity plugin id")
    @get:Input
    val id: Property<String> = objects.property()

    @get:Input
    @get:Optional
    val name: Property<String> = objects.property()

    @get:Input
    @get:Optional
    val version: Property<String> = objects.property()

    @get:Input
    @get:Optional
    val description: Property<String> = objects.property()

    @get:Input
    @get:Optional
    val url: Property<String> = objects.property()

    @get:Input
    val authors: ListProperty<String> = objects.listProperty()

    @get:Nested
    val dependencies: ListProperty<Dependency> = objects.listProperty()

    fun dependency(id: String, optional: Boolean = false) = dependencies.add(Dependency(id, optional))

    @get:Input
    val main: Property<String> = objects.property()

    override fun asConfigSerializable(): Any {
        return Serializable(this)
    }

    override fun setConventionsFromProjectMeta(project: Project) {
        id.convention(project.name)
        name.convention(project.name)
        description.convention(project.description)
        version.convention(project.version as String?)
    }

    override fun resourceFactory(): ResourceFactory {
        val factory = objects.newInstance(ConfigurateSingleFileResourceFactory.ObjectMapper::class)
        factory.json()
        factory.path.set("velocity-plugin.json")
        factory.value.set(this)
        return factory
    }

    @ConfigSerializable
    class Dependency(
        @Pattern(PLUGIN_ID_PATTERN, "Velocity plugin id (of dependency)")
        @get:Input
        val id: String,
        @get:Input
        val optional: Boolean = false
    )

    @ConfigSerializable
    class Serializable(json: VelocityPluginJson) {
        val id = json::id.getValidating()
        val name = json.name.orNull
        val version = json.version.orNull
        val description = json.description.orNull
        val url = json.url.orNull
        val dependencies = json.dependencies.nullIfEmpty()?.onEach { it::id.validate() }
        val main = json.main.get()
    }
}
