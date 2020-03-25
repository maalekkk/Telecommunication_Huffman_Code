package com.telekom3;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Scanner;

public class Server {

    private static HashMap<Character,String> huffmanCode = new HashMap<Character, String>();        //tworzenie HashMapy ktora przechowa slownik kodow Huffmana, znak oraz jego kod w stringu
    public static void printCode(HuffmanNode root, String s)                                        //funkcja rekurencyjna ktora pozwala nam zdobyc kod Huffmana dla danego znaku
    {
        if (root.left == null && root.right == null) {          //jesli oba wezly sa zerami to jest to lisc
            System.out.println(root.c + ":" + s);               //c jest znakiem natomiast s jest kodem Huffmana
            huffmanCode.put(root.c,s);                          //dodajemy do HashMapy pary (znak,kod Huffmana)
            return;
        }                                                       //w drzewie panuje zasada ze lewy wezel przyjmuje 0 natomiast prawy 1
        printCode(root.left, s + "0");                      //wywolania rekurencyjne dla lewego poddrzewa
        printCode(root.right, s + "1");                     //wywolanie rekurencyjne dla prawego poddrzewa
    }

    public static void main(String[] args) throws IOException {
        String content = null;
        try {
            content = new Scanner(new File("example.txt")).useDelimiter("\\Z").next();     //zczytujemy plik tekstowy, ktorego tresc chcemy wyslac, opcja \\Z pozwala zczytac caly plik, Scanner przechodzi po kazdym elemencie
        } catch(FileNotFoundException error) {                                                       //wyjatek gdy nie odczytamy pliku
            System.out.println("File not found!");
        }

        HashMap<Character, Integer> map = new HashMap<Character, Integer>();        //tworzymy hashmape by uniknac duplikaty znakow oraz zliczyc liczbę ich wystąpien
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);                                        //przechodzimy petla po stringu ktory okresla tekst z pliku
            map.merge(c, 1, Integer::sum);                              //funkcja scalajaca albo doda nowa pare albo powiekszy wartosc wystepowania danego znaku
        }

        PriorityQueue<HuffmanNode> q
                = new PriorityQueue<HuffmanNode>(map.size(), new HuffmanNodeComparator());      //tworzymy kolejke priorytetowa, ktora nie jest FIFO a ma zachowana kolejnosc(posortowana) w ten sposob ze elementy z najnisza wartoscia sa pierwsze brane do drzewa
        for (int i = 0; i < map.size(); i++) {
            HuffmanNode hn = new HuffmanNode();                 //tworzymy obiekt HuffmanNode(ktory ma znak wartosc) i moze miec wezly
            hn.c = (char) map.keySet().toArray()[i];            //przypisujemy obiektowi char
            hn.data = map.get(hn.c);                            //przypisujemy do danych wartosc kodu
            hn.left = null;                                     //przypisujemy wartosc null obu wezlom
            hn.right = null;
            q.add(hn);                                          //dodajemy do kolejki obiekt HuffmanNode
        }
        HuffmanNode root = null;                                //tworzymy obiekt ktory bedzie rootem czyli korzeniem
        while (q.size() > 1) {                              //bierzemy dwa obiekty z kolejki ktore maja najmniejsza ilosc wystapien, robimy to dopoki jest w kolejce wiecej elementow niz 1
            HuffmanNode x = q.peek();                       //pierwszy najmniejszy element z kolejki
            q.poll();                                       //usuwamy element z kolejki
            HuffmanNode y = q.peek();                       //drugi najmniejszy element z kolejki
            q.poll();                                       ////usuwamy element z kolejki
            HuffmanNode f = new HuffmanNode();              //tworzymy nowy obiekt ktory zlicza dwie wartosci
            f.data = x.data + y.data;                       //sumujemy dwie wartosci
            f.c = '-';                                      //nazwa znaku domyslna
            f.left = x;                                     //pierwszym dzieckiem jest najmniejszy element z kolejki
            f.right = y;                                    //drugim dzieckiem jest drugi najmniejszy element z kolejki
            root = f;                                       //nowy obiekt jest aktualnie korzeniem
            q.add(f);                                       //nowy obiekt wrzucamy do kolejki
        }
        System.out.println("Słownik: ");
        printCode(root, "");                //drukowanie na konsole przejscia po drzewie (slownik)
        StringBuilder compressedData = new StringBuilder();
        for(int i = 0; i < content.length();i++) {
            compressedData.append(huffmanCode.get(content.charAt(i)));                      //przechodzimy petla po naszym tekscie i zamieniamy kazdy znak na odpowiadaja mu wartosc kodu Huffmana
        }

        OutputStream outFile = new FileOutputStream("compressedOut.txt");            //tworzymy strumien wyjsciowy bajtow do pliku | bedzie zapisywac w pliku w pierwszej linii skompresowana wiadomosc natomiast w reszcie 2 linia lub wiecej slownik kodu Huffmana
        if (compressedData.length()%8 != 0) {                                              //jesli przygotowane do kompresji dane (ciag 0 i 1, ktore zostaly zamienione zamiast znakow) nie sa wielokrotnoscia 8 to musimy dopelnic do wielokrotnosci 8
            outFile.write((8 - compressedData.length() % 8)+48);                        //skompresowana wiadomosc polega na tym ze dzielimy na 8bitowe znaki z kodu ascii i wysylamy, musimy powiadomic odbiorce ile musi usunac z konca bitow ktore dodamy tylko po to zeby dalo sie wyslac ostatni pelny znak (8 bitow)
            compressedData.append(new String(new char[8 - compressedData.length() % 8]).replace("\0", "0"));        //dopelniamy 0 na koncu naszego ciagu 0 i 1. \0 to null i zamieniamy go na 0 z kodu ASCII, ilosc razy podana jest w nawiasie klamrowym
        }
        else {
            outFile.write(0);                                                           //jesli ciag bitow jest wielokrotnoscia 8 to zapisujemy 0 (tzn ze odbiorca nie usunie zadnego bitu)
        }
        for (int i = 0; i < compressedData.length(); i+=8) {                               //dokonujemy kompresji
            outFile.write(Integer.parseInt(compressedData.substring(i,i+8),2));      //bierzemy kazde kolejne 8 bitow i zamieniamy go na znak kodu ascii (nie interesuje nas jaki to bedzie znak)
        }
        outFile.write((int)'\n');                                                          //dodajemy przejscie do nowej linii, teraz bedzie zapisywany slownik kodu huffmana
        outFile.write(0x1C);                                                            //bedzie to znak informujacy ze to juz koniec czesci skompresowanej i bedzie zaczynal sie slownik
        for (int i = 0; i < huffmanCode.size(); i++) {                                     //przechodzimy po dlugosci naszego slownika
            char c = (char)huffmanCode.keySet().toArray()[i];                              //bierzemy znak czyli pierwszy element naszej pary
            outFile.write(0x1F);                                                        //aby oddzielnic znak od wartosci czyli od elementow danej pary uzywamy Unit Separatora z ascii (numer 1F w hex)
            outFile.write((int)c);                                                         //zapisujemy znak
            outFile.write(0x1F);
            String test = huffmanCode.get(c);
            for(int j = 0; j < test.length();j++)
                outFile.write((int)huffmanCode.get(c).toCharArray()[j]);                   //zapisujemy bajt po bajcie kod dla danego znaku
        }
        outFile.close();                                                                   //zamykamy nasz plik, jest przygotowany do przeslania

        ServerSocket ssock = new ServerSocket(5000);                //inicjalizujemy gniazdo sieciowe jako server, wybieramy port 5000
        Socket socket = ssock.accept();                                  //ackeptuje nowe polaczenie z socketem

        InetAddress IA = InetAddress.getByName("localhost");             //okreslamy adres IP

        File file = new File("compressedOut.txt");             //tworzymy obiekt File aby operowac na pliku
        FileInputStream fis = new FileInputStream(file);                 //tworzymy strumien ktory bedzie mogl operowac na bajtach
        BufferedInputStream bis = new BufferedInputStream(fis);          //tworzymy bufor dla danego strumienia by zmniejszyc dostep do pliku
        OutputStream os = socket.getOutputStream();                      //wyjscie gniazda sieciowego

        byte[] contents;
        long fileLength = file.length();
        long current = 0;
        while(current!=fileLength){                 //dopoki obecny wskaznik na wyslany
            int size = 10000;                       //maksymalna ilosc wyslania 10k znakow
            if(fileLength - current >= size)
                current += size;
            else{
                size = (int)(fileLength - current);  //jesli jest mniejsze niz 10k to bedzie to ostatnie wyslanie i wskaznik przechodzi na koniec czyli na dlugosc pliku
                current = fileLength;
            }
            contents = new byte[size];             //tworzymy tablice o rozmiarze jaki mamy wyslac
            bis.read(contents, 0, size);       //wczytujemy bajty ze strumienia do tablicy
            os.write(contents);                    //do wyjscia gniazda sieicowego zapisujemy cala tablice
            System.out.print("Sending file ... "+(current*100)/fileLength+"% complete!\n");
        }
        os.flush();                                 //przeslanie danych do socketu
        socket.close();                             //zamkniecie gniazda
        ssock.close();
        System.out.println("File sent succesfully!");
    }
}
