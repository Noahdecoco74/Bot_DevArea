package devarea.automatical;

import devarea.Main;
import devarea.commands.Command;
import devarea.data.ColorsUsed;
import devarea.data.TextMessage;
import devarea.event.MemberJoin;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;

import java.util.Objects;
import java.util.function.Consumer;

public class Joining {

    private final Member member;
    private final TextChannel textChannel;
    private final Message message;
    private Boolean ended = false;

    private final ReactionEmoji no;
    private final ReactionEmoji yes;

    private int status;

    public Joining(final Member member) {
        this.member = member;
        this.status = 0;
        this.yes = ReactionEmoji.custom(Main.idYes);
        this.no = ReactionEmoji.custom(Main.idNo);
        System.out.println("Actualy before create");
        Main.devarea.getChannelById(Snowflake.of("843823896222629888")).block().addMemberOverwrite(member.getId(), PermissionOverwrite.forMember(member.getId(), PermissionSet.of(), PermissionSet.of(Permission.VIEW_CHANNEL))).subscribe();
        this.textChannel = Main.devarea.createTextChannel(textChannelCreateSpec -> {
            textChannelCreateSpec.setName(member.getDisplayName());
            textChannelCreateSpec.setParentId(Main.idCategoryJoin);
            textChannelCreateSpec.setTopic("Petit questionnaire d'arrivé !");
        }).block();
        this.textChannel.addMemberOverwrite(member.getId(), PermissionOverwrite.forMember(member.getId(), PermissionSet.of(Permission.VIEW_CHANNEL, Permission.READ_MESSAGE_HISTORY), PermissionSet.of(Permission.ADD_REACTIONS, Permission.SEND_MESSAGES))).subscribe();
        this.message = sendEmbed(embed -> {
            embed.setTitle("Bienvenue " + member.getDisplayName() + " sur Dev'Area !");
            embed.setDescription(TextMessage.firstText);
            embed.setColor(ColorsUsed.just);
        }, true);
        this.message.addReaction(this.yes).subscribe();

        new Thread(() -> {
            try {
                Thread.sleep(1000 * 60 * 10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!this.ended) {
                try {
                    this.member.kick("Didn't finish QUIZZ").block();
                } catch (Exception ignored) {
                }
            }
        }).start();

    }

    final Object obj = new Object();

    public void next(final ReactionAddEvent event) {
        synchronized (obj) {
            if (!event.getMessageId().equals(this.message.getId()))
                return;

            boolean choice;
            if (event.getEmoji().equals(ReactionEmoji.custom(Main.idYes)) || event.getEmoji().equals(ReactionEmoji.custom(Main.idNo)))
                choice = event.getEmoji().equals(ReactionEmoji.custom(Main.idYes));
            else
                return;

            if (this.status == 0) {
                this.message.edit(msg -> msg.setEmbed(embed -> {
                    embed.setTitle("Pourquoi es-tu là ?");
                    embed.setDescription("    - Es-tu développeur ou es-tu ici pour apprendre à développer -> <:ayy:" + Main.idYes.getId().asString() + ">\n    - Es-tu là car tu as besoin de développeurs, tu as une mission à donner -> <:ayy:" + Main.idNo.getId().asString() + ">");
                    embed.setColor(ColorsUsed.just);
                })).subscribe();
                this.message.addReaction(this.no).subscribe();
            } else if (this.status == 1) {

                if (choice)
                    this.message.edit(msg -> msg.setEmbed(embed -> {
                        embed.setTitle("Conseils de communications du code");
                        embed.setDescription(TextMessage.rulesForSpeakCode);
                        embed.setColor(ColorsUsed.just);
                    })).subscribe();

                else {
                    final PermissionOverwrite over = PermissionOverwrite.forMember(this.member.getId(), PermissionSet.of(Permission.VIEW_CHANNEL, Permission.READ_MESSAGE_HISTORY), PermissionSet.of());
                    Main.devarea.getChannelById(Main.idMissionsGratuites).block().addMemberOverwrite(this.member.getId(), over).subscribe();
                    Main.devarea.getChannelById(Main.idMissionsPayantes).block().addMemberOverwrite(this.member.getId(), over).subscribe();

                    this.message.edit(msg -> msg.setEmbed(embed -> {
                        embed.setTitle("Conseils pour demander du code (missions)");
                        embed.setDescription(TextMessage.rulesForAskCode);
                        embed.setColor(ColorsUsed.just);
                    })).subscribe();
                }
                this.message.removeReaction(this.no, Main.client.getSelfId()).subscribe();

            } else if (this.status == 2) {
                this.message.edit(msg -> msg.setEmbed(embed -> {
                    embed.setTitle("Règles");
                    embed.setDescription(TextMessage.rules);
                    embed.setColor(ColorsUsed.just);
                })).subscribe();
            } else if (this.status == 3) {
                final PermissionOverwrite over = PermissionOverwrite.forMember(this.member.getId(), PermissionSet.of(Permission.VIEW_CHANNEL, Permission.READ_MESSAGE_HISTORY), PermissionSet.of());
                Main.devarea.getChannelById(Main.idPresentation).block().addMemberOverwrite(this.member.getId(), over).subscribe();

                this.message.edit(msg -> msg.setEmbed(embed -> {
                    embed.setTitle("Avant de commencer !");
                    embed.setDescription(TextMessage.presentation);
                    embed.setColor(ColorsUsed.just);
                })).subscribe();

            } else {
                this.message.removeAllReactions().subscribe();

                final PermissionOverwrite over = PermissionOverwrite.forMember(this.member.getId(), PermissionSet.of(Permission.VIEW_CHANNEL, Permission.READ_MESSAGE_HISTORY), PermissionSet.of());
                Main.devarea.getChannelById(Main.idRolesChannel).block().addMemberOverwrite(this.member.getId(), over).subscribe();

                this.message.edit(msg -> msg.setEmbed(embed -> {
                    embed.setTitle("Et le plus important...");
                    embed.setDescription(TextMessage.roles);
                    embed.setColor(ColorsUsed.just);
                })).subscribe();
                this.ended = true;
                new Thread(() -> {
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (this.member.getGuild().block() != null) {

                    }

                    this.member.addRole(Main.idRoleRulesAccepted).subscribe();
                    this.disconnect();

                    try {
                        this.member.getPrivateChannel().block().createEmbed(TextMessage.helpEmbed).subscribe();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    ((TextChannel) Main.devarea.getChannelById(Main.idWelcomChannel).block()).createMessage(msg -> msg.setEmbed(embed -> {
                        embed.setTitle("Salut ! " + this.member.getTag() + ", bienvenue sur **Dev'Area**, amuse-toi bien !");
                        embed.setDescription("Membre n°" + Main.devarea.getMembers().buffer().blockLast().size());
                        embed.setImage(this.member.getAvatarUrl());
                        embed.setColor(ColorsUsed.just);
                    })).subscribe();

                    ((TextChannel) Main.devarea.getChannelById(Main.idGeneralChannel).block())
                            .createMessage(msg -> msg
                                    .setContent("<@" + this.member.getId().asString() + "> a passé le petit questionnaire d'arrivée ! Vous pouvez lui souhaiter la bienvenue !"))
                            .subscribe();

                }).start();
            }

            try {
                Objects.requireNonNull(event.getMessage().block()).removeReaction(event.getEmoji(), event.getUserId()).subscribe();
            } catch (Exception e) {
                e.printStackTrace();
            }

            this.status++;
        }
    }

    public void disconnect() {
        try {
            this.ended = true;
            MemberJoin.bindJoin.remove(this.member.getId());
            textChannel.delete().subscribe();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected Message send(final Consumer<? super MessageCreateSpec> spec, boolean block) {
        return Command.send(this.textChannel, spec, block);
    }

    protected Message sendEmbed(final Consumer<? super EmbedCreateSpec> spec, boolean block) {
        return send(msg -> msg.setEmbed(spec), block);
    }

}
