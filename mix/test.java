import java.util.Scanner;
public class test {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String name;
        System.out.println("gib deinen namen ein: ");
        name = scanner.nextLine();
        System.out.println("Dein name ist "+ name );
        double dblZahl1;
        System.out.println("Zahlenraten Spiel!");
        System.out.println("gib eine Zahl ein: ");
        dblZahl1 = scanner.nextDouble();
        if (dblZahl1 == 8){
            System.out.println("Super richtige zahl");
        }
else{
    System.out.println("Nicht richtige zahl");
}

double note;
System.out.println("gib deine note ein: ");
note = scanner.nextDouble(); 
if(note <3) {
    System.out.println("gut");
} else {
    System.out.println("Schlecht");
}
    }
}