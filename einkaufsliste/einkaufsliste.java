import java.util.Scanner;


public class einkaufsliste {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String Produkt;
        double Preis;
        int Anzahl;
        double gesamt;
        System.out.println("Was willst du kaufen?");
        Produkt = scanner.nextLine();
        System.out.println("Wieviel kostet eins?");
        Preis= scanner.nextDouble();
        System.out.println("Wie viele?");
        Anzahl= scanner.nextInt();
        System.out.println("Du hast "+ Anzahl +" "+ Produkt + " gekauft");
        gesamt=Anzahl*Preis;
        System.out.println("Gesamt:"+gesamt);
     
    }
}
        


