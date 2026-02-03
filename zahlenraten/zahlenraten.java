import java.util.Scanner;
public class zahlenraten {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int zahlZumRaten = 9;
        int versuch;
        System.out.println("Ich habe mir eine Zahl zwischen 1 und 100 ausgedacht. Kannst du sie erraten?");
        versuch = scanner.nextInt();
        if (versuch == zahlZumRaten) {
            System.out.println("Richtig! Du hast die Zahl erraten.");
        } else {
            System.out.println("Leider falsch. Versuche es noch einmal!");
        }
    }
}
