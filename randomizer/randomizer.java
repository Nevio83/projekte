import java.util.Random;

public class randomizer {
    public static void main(String[] args) {
        Random random = new Random();
        int zahl;
        boolean Muenze;
        zahl = random.nextInt(1,7);
        System.out.println(zahl);
        Muenze =random.nextBoolean();
        if(Muenze)
        { System.out.println("Kopf");
    }
    else{
        System.out.println("Zahl");
    
        }
    }
}