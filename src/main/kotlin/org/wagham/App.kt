import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.Member
import dev.kord.core.entity.component.UnknownComponent
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.SelectMenuInteractionCreateEvent
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.on
import dev.kord.core.supplier.CacheEntitySupplier
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.interaction.int
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.modify.InteractionResponseModifyBuilder
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed
import kotlinx.coroutines.flow.*


@OptIn(PrivilegedIntent::class)
suspend fun main() {
    val kord = Kord(System.getenv("DISCORD_TOKEN"))
    val cache = CacheEntitySupplier(kord)
    val roleMap = mutableMapOf<Snowflake, String>()

    kord.createGlobalChatInputCommand(
        "get_roles",
        "A slash command that edit roles"
    )

    kord.createGlobalChatInputCommand(
        "get_users",
        "A slash command that gets all the users with their roles"
    )


    kord.createGlobalChatInputCommand(
        "lol_button",
        "A slash command that doesn't do anything"
    )

    kord.createGuildChatInputCommand(
        Snowflake(867839810395176960),
        "sum",
        "A slash command that sums two numbers"
    ) {
        int("first_number", "The first operand") {
            required = true
            for(i in 0L..9L) {
                choice("$i", i)
            }
        }
        int("second_number", "The second operand") {
            required = true
            for(i in 0L..9L) {
                choice("$i", i)
            }
        }
    }

    kord.on<SelectMenuInteractionCreateEvent> {
        val response = interaction.deferPublicMessageUpdate()
        roleMap[interaction.message.id] = interaction.values.first()
        response.edit {
            embed {
                title = "Ho editato il messaggio"
                description = "Il ruolo selezionato è: ${interaction.values.joinToString { it }}"
            }
        }
    }

    kord.on<ReactionAddEvent> {
        message.addReaction(emoji)
    }

    kord.on<ButtonInteractionCreateEvent> {
        val response = interaction.deferPublicMessageUpdate()
        val role = roleMap[interaction.message.id]
        if (role == null) {
            response.edit {
                embed {
                    title = "Non hai selezionato alcun ruolo"
                    description = "Fuck you!"
                }
            }
        } else {
            val realRole = cache.roles.first{ it.name == role }
            val currentRoles = interaction.data.member.value?.roles?.toMutableSet() ?: mutableSetOf()
            val memberData = interaction.data.member.value!!
            if (currentRoles.firstOrNull{ it == realRole.id } != null) {
                response.edit {
                cache.getMember(memberData.guildId, memberData.userId)
                    .edit {
                        roles = currentRoles.apply { remove( realRole.id ) }
                    }
                    embed {
                        title = "Ti è stato tolto un ruolo"
                        description = role
                    }
                    components = mutableListOf()
                }
            } else {
                cache.getMember(memberData.guildId, memberData.userId)
                    .edit {
                        roles = currentRoles.apply { add( realRole.id ) }
                    }
                response.edit {
                    embed {
                        title = "Ti è stato aggiunto un ruolo"
                        description = role
                    }
                    components = mutableListOf()
                }
            }
        }
    }

    kord.on<GuildChatInputCommandInteractionCreateEvent> {
        val response = interaction.deferPublicResponse()
        val command = interaction.command
        val responseBuilder = when (interaction.invokedCommandName) {
            "sum" -> {
                val first = command.integers["first_number"]!! // it's required so it's never null
                val second = command.integers["second_number"]!!

                val ret: InteractionResponseModifyBuilder.() -> Unit =  {
                    embed {
                        title = "You called sum"
                        description = "$first + $second = ${first + second}"
                    }
                }
                ret

            }
            "get_users" -> {
                val guild = cache.getGuild(interaction.guildId)
                val members = guild.members.filter { !it.isBot }.fold(mapOf<String, String>()) { acc, it -> acc + (it.displayName to it.roles.map { it.name }.toList().joinToString { r -> r })}
                val ret: InteractionResponseModifyBuilder.() -> Unit = {
                    embed {
                        title = "Utenti del server"
                        description = "Unitevi!"
                        members.onEach { m ->
                            field {
                                name = m.key
                                value = if (m.value.length >0 ) m.value else "Nessuno"
                            }
                        }
                    }
                }
                ret
            }
            "get_roles" -> {
                val roleNames = interaction.data.member.value
                    ?.roles
                    ?.mapNotNull {
                        cache.getRole(it)?.name
                    } ?: listOf()

                val guildRoles = cache.getGuild(interaction.guildId).roles.toList()

                val ret: InteractionResponseModifyBuilder.() -> Unit = {
                    embed {
                        title = "You called roles"
                        description = roleNames.joinToString { it }
                    }
                    actionRow {
                        selectMenu("guildRoles") {
                            guildRoles.map {
                                option(it.name, it.name) {
                                    description = it.name
                                }
                            }
                        }
                    }
                    actionRow {
                        interactionButton(ButtonStyle.Primary, "guildRolesButton") {
                            label = "Toggle"
                        }
                    }
                }
                ret

            }
            "lol_button" -> {
                val ret: InteractionResponseModifyBuilder.() -> Unit = {
                    embed {
                        title = "This is a button"
                    }
                    actionRow {
                        interactionButton(ButtonStyle.Primary, "guildRolesButton") {
                            label = "Button"
                        }
                    }
                }
                ret
            }

            else -> {
                val ret: InteractionResponseModifyBuilder.() -> Unit = {
                    embed {
                        title = "Error"
                        description = "Command does not exists"
                    }
                }
                ret
            }
        }

        response.respond(responseBuilder)
    }

    kord.login {
        intents += Intent.GuildMembers
        intents += Intent.GuildPresences
        intents += Intent.MessageContent
    }
}