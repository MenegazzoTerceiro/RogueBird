import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Sound {

    private static final Map<String, Clip> sounds = new HashMap<>();

    private static final String PATH = "/recursos/efeitos sonoros/";

    public static void load(String name) {

        if (sounds.containsKey(name)) {
            return;
        }

        try {

            String path = PATH + name + ".wav";

            InputStream input = Sound.class.getResourceAsStream(path);

            if (input == null) {
                throw new RuntimeException("Som não encontrado: " + path);
            }

            AudioInputStream audio = AudioSystem.getAudioInputStream(input);

            Clip clip = AudioSystem.getClip();

            clip.open(audio);

            sounds.put(name, clip);

            audio.close();
            input.close();

        } catch (Exception e) {

            throw new RuntimeException("Erro ao carregar o som: " + name, e);

        }
    }

    public static void play(String name) {

        Clip clip = sounds.get(name);

        if (clip == null) {
            System.out.println("Som não carregado: " + name);
            return;
        }

        clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    public static void stop(String name) {

        Clip clip = sounds.get(name);

        if (clip != null) {
            clip.stop();
            clip.setFramePosition(0);
        }
    }

    public static void stopAll() {

        for (Clip clip : sounds.values()) {
            clip.stop();
            clip.setFramePosition(0);
        }
    }
}