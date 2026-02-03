import java.util.Scanner;

public class name {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Wie heisst du?");
        String name = scanner.nextLine();
        System.out.println("Wie alt bist du?");
        int alter = scanner.nextInt();
        System.out.println("Hallo " + name +",naechstetes Jahr wirst du " + (alter + 1) + " Jahre alt.");
        scanner.close();
    }
}
        