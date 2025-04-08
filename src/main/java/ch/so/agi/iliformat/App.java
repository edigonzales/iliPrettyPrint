package ch.so.agi.iliformat;

import java.io.File;

public class App {

    public static void main(String[] args) {
        System.out.println("Hallo Welt.");
        
        ImportInterlis.readIliFile(new File[] {new File("src/test/data/SO_ARP_SEin_Konfiguration_20250115.ili")});
        
        
    }
}
