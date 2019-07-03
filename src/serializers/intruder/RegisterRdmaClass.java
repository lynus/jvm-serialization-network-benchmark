package serializers.intruder;

import data.media.Image;
import data.media.Media;
import data.media.MediaContent;
import intruder.Factory;

import java.util.ArrayList;
import java.util.List;

public class RegisterRdmaClass {
    public static void registerType() {
        Factory.registerRdmaClass(Media.class);
        Factory.registerRdmaClass(MediaContent.class);
        Factory.registerRdmaClass(Image.class);
        Factory.registerRdmaClass(String.class);
        Factory.registerRdmaClass(List.class);
        Factory.registerRdmaClass(Media.Player.class);
        Factory.registerRdmaClass(Image.Size.class);
        Factory.registerRdmaClass(Integer.class);
        Factory.registerRdmaClass(Long.class);
        Factory.registerRdmaClass(Object.class);
        Factory.registerRdmaClass(ArrayList.class);

    }
}
