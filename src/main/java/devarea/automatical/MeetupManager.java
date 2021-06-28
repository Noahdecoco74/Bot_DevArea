package devarea.automatical;

import devarea.Main;
import devarea.commands.Command;
import devarea.commands.object_for_stock.MeetupStock;
import devarea.data.ColorsUsed;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.reaction.ReactionEmoji;

import java.io.*;
import java.time.Instant;
import java.util.*;

public class MeetupManager {

    public static HashMap<Message, MeetupStock> messageBoundToMeetup = new HashMap<>();

    public static HashMap<MeetupStock, Message> messageSended = new HashMap<>();

    public static void init() {
        File file = new File("./meetup.devarea");
        if (file.exists()) {
            try {
                FileInputStream in = new FileInputStream(file);
                ObjectInputStream inObj = new ObjectInputStream(new ByteArrayInputStream(in.readAllBytes()));
                HashMap<MeetupStock, MessageSeria> readed = (HashMap<MeetupStock, MessageSeria>) inObj.readObject();
                readed.forEach((meetupStock, s) -> messageSended.put(meetupStock, s.getMessage()));
                in.close();
                inObj.close();
                System.out.println("Read meetup successfully !");
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                System.out.println("Data are lost !!!!");
            }
        }
        new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(60000);
                    Date date = new Date();
                    messageSended.forEach((meetupStock, message) -> {
                        if (!meetupStock.getAlreayMake()) {
                            if (meetupStock.getDate().after(date)) {
                                Command.send((TextChannel) message.getChannel().block(), msg -> {
                                    msg.setContent("Un meetup a commencé avec sujet : " + meetupStock.getDescription() + ".\n<@&" + Main.idPingMeetup + ">");
                                }, false);
                                Main.devarea.createVoiceChannel(voiceChannelCreateSpec -> {
                                    voiceChannelCreateSpec.setParentId(Main.idCategoryGeneral);
                                    voiceChannelCreateSpec.setName("Meetup by " + Main.devarea.getMemberById(meetupStock.getAuthor()).block().getDisplayName());
                                }).subscribe();
                                meetupStock.setAlreadyMake(true);
                            }
                        }
                    });
                }
            } catch (Exception e) {
            }
        }).start();
    }

    public static void save() {
        try {
            File file = new File("./meetup.devarea");
            FileOutputStream out = new FileOutputStream(file);
            ByteArrayOutputStream outByte = new ByteArrayOutputStream();
            ObjectOutputStream outObj = new ObjectOutputStream(outByte);
            HashMap<MeetupStock, MessageSeria> toWrite = new HashMap<>();
            messageSended.forEach((meetupStock, message) -> toWrite.put(meetupStock.getForWrite(), new MessageSeria(message)));
            outObj.writeObject(toWrite);
            outObj.flush();
            out.write(outByte.toByteArray());
            out.flush();
            out.close();
            System.out.println("Data writed successfully !");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addMeetupAtValide(MeetupStock meetup) {
        TextChannel meetupVerif = (TextChannel) Main.devarea.getChannelById(Main.idMeetupVerif).block();
        Message message = Command.sendEmbed(meetupVerif, meetup.getEmbed(), true);
        messageBoundToMeetup.put(message, meetup);
        addYesAndNo(message);
    }

    public static void addYesAndNo(Message message) {
        message.addReaction(ReactionEmoji.custom(Main.idYes)).subscribe();
        message.addReaction(ReactionEmoji.custom(Main.idNo)).subscribe();
    }

    public static void addYes(Message message) {
        message.addReaction(ReactionEmoji.custom(Main.idYes)).subscribe();
    }

    public static void getEvent(ReactionAddEvent event) {
        for (Map.Entry<Message, MeetupStock> entry : messageBoundToMeetup.entrySet()) {
            Message message = entry.getKey();
            MeetupStock meetup = entry.getValue();
            if (message.getId().equals(event.getMessageId())) {
                if (event.getEmoji().equals(ReactionEmoji.custom(Main.idYes))) {
                    try {
                        Main.devarea.getMemberById(meetup.getAuthor()).block().getPrivateChannel().block().createEmbed(embedCreateSpec -> {
                            embedCreateSpec.setTitle("Votre meetup a été accepté !");
                            embedCreateSpec.setTimestamp(Instant.now());
                            embedCreateSpec.setColor(ColorsUsed.just);
                        }).subscribe();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    messageBoundToMeetup.remove(message);
                    Message message1 = ((TextChannel) Main.devarea.getChannelById(Main.idMeetupAnnonce).block()).createEmbed(meetup.getEmbed()).block();
                    addYes(message1);
                    messageSended.put(meetup, message1);
                    save();
                } else if (event.getEmoji().equals(ReactionEmoji.custom(Main.idNo))) {
                    try {
                        Main.devarea.getMemberById(meetup.getAuthor()).block().getPrivateChannel().block().createEmbed(embedCreateSpec -> {
                            embedCreateSpec.setTitle("Votre meetup a été rejeté !");
                            embedCreateSpec.setTimestamp(Instant.now());
                            embedCreateSpec.setColor(ColorsUsed.wrong);
                        }).block();
                    } catch (Exception e) {
                    }
                    messageBoundToMeetup.remove(message);
                }
                Command.delete(false, message);
            }
        }
    }

    public static List<MeetupStock> getMeetupsFrom(Snowflake memberID) {
        List<MeetupStock> list = new ArrayList<>();
        messageBoundToMeetup.forEach((message, meetupStock) -> {
            if (meetupStock.getAuthor().equals(memberID))
                list.add(meetupStock);
        });
        messageSended.forEach((meetupStock, message) -> {
            if (meetupStock.getAuthor().equals(memberID))
                list.add(meetupStock);
        });
        return list;
    }

    public static void remove(MeetupStock meetup) {
        for (Map.Entry<Message, MeetupStock> entry : messageBoundToMeetup.entrySet()) {
            Message key = entry.getKey();
            MeetupStock value = entry.getValue();
            if (value.equals(meetup)) {
                Command.delete(false, key);
                messageBoundToMeetup.remove(key);
            }
        }
        for (Map.Entry<MeetupStock, Message> entry : messageSended.entrySet()) {
            MeetupStock meetupStock = entry.getKey();
            Message message = entry.getValue();
            if (meetupStock.equals(meetup)) {
                Command.delete(false, message);
                messageSended.remove(meetupStock);
                save();
            }
        }
    }
}
