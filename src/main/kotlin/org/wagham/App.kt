import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.Guild
import dev.kord.core.event.interaction.GlobalApplicationCommandInteractionCreateEvent
import dev.kord.core.event.interaction.GlobalChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.core.supplier.CacheEntitySupplier
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.interaction.int
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.modify.embed


@OptIn(PrivilegedIntent::class)
suspend fun main() {
    val kord = Kord(System.getenv("DISCORD_TOKEN"))
    val cache = CacheEntitySupplier(kord)

    kord.createGlobalChatInputCommand(
        "get_roles",
        "A slash command that sums two numbers"
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

    kord.on<GuildChatInputCommandInteractionCreateEvent> {
        val response = interaction.deferPublicResponse()
        val command = interaction.command
        val currentGuild = cache.getGuild(interaction.guildId)
        val responseEmbed = when (interaction.invokedCommandName) {
            "sum" -> {
                val first = command.integers["first_number"]!! // it's required so it's never null
                val second = command.integers["second_number"]!!

                val builder = EmbedBuilder()
                builder.title = "Ruoli dell'utente"
                builder.description = "$first + $second = ${first + second}"
                builder

            }
            "get_roles" -> {
                val roleNames = interaction.data.member.value
                    ?.roles
                    ?.map {
                        currentGuild.getRole(it).name
                    } ?: listOf()

                val builder = EmbedBuilder()
                builder.title = "Ruoli dell'utente"
                builder.description = roleNames.joinToString { it }
                builder
            }
            else -> {
                val builder = EmbedBuilder()
                builder.title = "Errore"
                builder.description = "Comando non valido"
                builder
            }
        }

        response.respond {
            embed {
                title = responseEmbed.title
                description = responseEmbed.description
            }
        }
    }

    kord.login {
        intents += Intent.GuildMembers
        intents += Intent.GuildPresences
        intents += Intent.MessageContent
    }
}