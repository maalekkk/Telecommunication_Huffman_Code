package com.telekom3;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;

public class Client {

    public static void main(String[] args) throws IOException {
        //Inicjalizacja gniazda sieciowego (socket), podajemy adres sieciowy IP oraz port sieciowy
        Socket socket = new Socket(InetAddress.getByName("localhost"), 5000);
        //Socket socket = new Socket(InetAddress.getByName("192.168.10.10"), 5000);
        byte[] contents = new byte[10000]; //Bufor dla przyjmowanych danych

        //Inicjalizacja pliku wyjściowego
        FileOutputStream fos = new FileOutputStream("compressedIn.txt");
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        InputStream is = socket.getInputStream();

        //Liczba bajtów odczytanych w jednym wywołaniu read()
        int bytesRead = 0;

        //Odbieranie przekazywanych danych przez gniazdo i zapisywanie ich do pliku
        while((bytesRead=is.read(contents))!=-1)
            bos.write(contents, 0, bytesRead);

        bos.flush(); //Metoda przekazuje buforowane bajty wyjściowe do bazowego strumienia wyjściowego.
        socket.close(); //Zamknięcie gniazda

        System.out.println("File saved successfully!");

        String messageInBits = ""; //Zmienna przechowująca w formie binarnej zakodowane dane
        InputStream inputStream = new FileInputStream("compressedIn.txt"); //Plik zawierający zakodowane dane oraz słownik
        int byteRead, byteRead2, byteRead3; //Pomocnicze zmienne do wykrycia sekwencji 3 znakow odzielających część zakodowaną od słownika
        int extraBits = inputStream.read();  //wczytanie ilosci extra bitow ktore zostaly dodane przy wysylaniu
        extraBits = (extraBits == 0) ? 0 : extraBits - 48;
        while((byteRead = inputStream.read()) != -1) {
            if(byteRead == 10) { //Sprawdzamy czy natrawfilismy na pierwszy znak naszej sekwencji seperatora
                byteRead2 = inputStream.read();
                if(byteRead2 == 28){ //Sprawdzamy czy natrawfilismy na drugi znak naszej sekwencji seperatora
                    byteRead3 = inputStream.read();
                    if(byteRead3 == 31) { //Sprawdzamy czy natrawfilismy na trzeci (ostatni) znak naszej sekwencji seperatora
                        break; //jeśli tak kończymy wczytywanie zakodowanych danych
                    }
                    else { // jeśli nie wczytujemy dalej zakodowane dane
                        messageInBits += DECtoBIN(byteRead);
                        messageInBits += DECtoBIN(byteRead2);
                        messageInBits += DECtoBIN(byteRead3);
                    }
                }
                else { // jeśli nie wczytujemy dalej zakodowane dane
                    messageInBits += DECtoBIN(byteRead);
                    messageInBits += DECtoBIN(byteRead2);
                }
            }
            else // jeśli nie wczytujemy dalej zakodowane dane
                messageInBits += DECtoBIN(byteRead);
        }
        messageInBits = messageInBits.substring(0,messageInBits.length()-extraBits); // usunięcie bitów wypeniających

        String dictionary = "";
        while((byteRead = inputStream.read()) != -1) {
            dictionary += Character.toString(byteRead); //wczytanie słownika
        }

        //Mapa przechowująca nasz słownik w klucz(znak) oraz w wartości(kod Huffmana)
        HashMap<String,String> huffmanCode= new HashMap<String, String>();
        String[] parts = dictionary.split(Character.toString(0x1F));  //rodzielanie znaków od ich kodów                                              //podzielenie linii slownika na stringi dzielac po ';'
        for(int i=0; i<parts.length;i+=2) {
            huffmanCode.put(parts[i],parts[i+1]);    //wpisanie do hashMapy wartosci
        }

        huffmanCode = sortByValue(huffmanCode); //Sortowanie po długości kodu w słowniku (od najdłuższych do najkrótszych)

        System.out.println("Słownik:");
        System.out.println(huffmanCode); //Wypisanie słownika

        String word;
        StringBuilder decompressedData = new StringBuilder(); //Zmienna przechowująca zdekodowane dane
        int idx = 0;
        //Odkodowyanie danych
        for(int i = 0; i < messageInBits.length(); ) {
            for(int j = 0; j < huffmanCode.size();j++) {
                word = huffmanCode.get(huffmanCode.keySet().toArray()[j].toString());  //Wyciągamy kod z hashMapy (pamiętając że hashMapa jest posortowana najdłuszego kodu do najkrótszego)
                idx = i+word.length(); //Pomocniczo zapisujemy długość aktualnego kodu dodaną do aktualnej wartości iterator
                if(idx <= messageInBits.length() && messageInBits.substring(i,idx).equals(word))  { //sprawdzamy czy dany kod jest równy wyciętemu fragmentowi z zakododowanych danych
                    i = idx; //Przeskocznie iteracji o długość słowa
                    decompressedData.append(huffmanCode.keySet().toArray()[j]); //Zapisujemy wartość klucza czyli nasz znak którego kod został dopasowany
                    break;
                }
            }
        }

        System.out.println("Odkodowane dane:");
        System.out.println(decompressedData);

        PrintWriter pw = null;
        try {
            pw = new PrintWriter("decompressed.txt");
        } catch (FileNotFoundException error) {
            System.out.println(error);
        }
        pw.print(decompressedData); //wypisanie do pliku zdekodowanych danych
        pw.close();
    }

    //Metoda sortująca Hashmape malejąco po długości kodów Huffmana
    public static HashMap<String, String> sortByValue(HashMap<String, String> hm)
    {
        // Tworzymy listę z elementów HashMapy
        List<Map.Entry<String, String> > list =
                new LinkedList<Map.Entry<String, String> >(hm.entrySet());

        // Sortujemy malejąco liste po długości kodów Huffmana
        Collections.sort(list, new Comparator<Map.Entry<String, String>>() {
            public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2)
            {
                return (o1.getValue().length() - o2.getValue().length())*(-1); //odejmowanie odsiebie długości kodów
            }
        });

        // Umieszczamy dane z posortowanej listy w hashmapie
        HashMap<String, String> temp = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    //Zamiana liczby w systemie dziesiętnym do systemu binarnego
    private static String DECtoBIN(int dec) {
        String temp = "";
        while(dec > 0)
        {
            int y = dec % 2;
            temp = y+temp ;
            dec = dec / 2;
        }
        temp = new String(new char[8-temp.length()]).replace("\0", "0") + temp;
        return temp;
    }
}
