import StringMethods.StemmerPorterRU;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static String FILENAME_DATABASE = "Database.ser";
    public static PairDatabase DATABASE = new PairDatabase();

    public static void test_read_from_file() throws IOException {
        File f = new File("Запросы.txt");
        final int length = (int) f.length();

        String str = "";

        if (length != 0) {

            char[] cbuf = new char[length];

            InputStreamReader isr = new InputStreamReader(new FileInputStream(f), "UTF-8");

            final int read = isr.read(cbuf);

            str = new String(cbuf, 0, read);
            isr.close();
        }

        newPairDatabase(str);
    }


    public static void main(String[] args) throws IOException {

        long startTime = System.currentTimeMillis();

        test_read_from_file();

        System.out.println(DATABASE.edit);
        System.out.println(DATABASE.orig);
        debuggingKeywords();
        writePairDatabase();
        clearPairDatabase();
        readPairDatabase();
        debuggingKeywords();
        System.out.println("Время выполнения (сек): " + (double) (System.currentTimeMillis() - startTime)/1000);
/*
        //region Запись базы
        try {
            FileOutputStream fileOut =
                    new FileOutputStream(FILENAME_DATABASE);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(wordHash.edit);
            out.close();
            fileOut.close();
        } catch (IOException i) {
            i.printStackTrace();
        }
        //endregion

        //region Чтение
        HashMap<String, Integer> e = null;
        try {
            FileInputStream fileIn = new FileInputStream(FILENAME_DATABASE);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            e = (HashMap<String,Integer>) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException i) {
            i.printStackTrace();
        } catch (ClassNotFoundException c) {
            System.out.println("HashMap<String, Integer> not found");
            c.printStackTrace();
        }
        return;
        //endregion
*/
    }
    static class PairDatabase implements Serializable {
        HashMap<String, Integer> edit = new HashMap<String, Integer>();
        HashMap<String, String> orig = new HashMap<String, String>();

        public HashMap<String, Integer> getEdit() {
            return edit;
        }

        public HashMap<String, String> getOrig() {
            return orig;
        }

        PairDatabase(){}
        PairDatabase(HashMap<String, Integer> edit, HashMap<String, String> orig) {
            this.edit = edit;
            this.orig = orig;
        }
    }

    public static void debuggingKeywords(){
        System.out.println("[!] НАЧАЛО ОТЛАДКИ БАЗЫ");
        int quantityAllWords = DATABASE.edit.size();
        int quantityPendingWords = 0;
        for(Map.Entry<String, Integer> entry : DATABASE.edit.entrySet())
            if (entry.getValue() > 0)
                quantityPendingWords++;
        if (quantityPendingWords == 0){
            System.out.println("[i] В базе не содержится слов требующие обработку");
            return;
        }
        System.out.println("*** Кол-во слов в базе: " + quantityAllWords + "\n*** Ожидающих обработку: " + quantityPendingWords);
        System.out.println("Ключевые ли следующие слова...\n" +
                "\"1\" - ДА\n" +
                "\"2\" - НЕТ\n" +
                "\"0\" - Остановить отладку\n");
        int i = 1;
        Scanner scanner = new Scanner(System.in);
        for(Map.Entry<String, Integer> entry : DATABASE.edit.entrySet()){
            if(entry.getValue() > 0){
                System.out.print("(" + i + "/" + quantityPendingWords + ") "+ entry.getKey() + " ("+DATABASE.orig.get(entry.getKey())+"): " );
                switch (scanner.nextInt()){
                    case 1:
                        entry.setValue(0);
                        break;
                    case 2:
                        entry.setValue(-1);
                        break;
                    case 0:
                        System.out.println("[i] Отладка остановлена. В базе еще " + (quantityPendingWords - i + 1) + " необработанных слов");
                        return;
                }
                i++;
            }
        }
        System.out.println("[i] Отлично! Все слова обработаны");
    }

    public static HashMap<String, Integer> removeSelectEntries(HashMap<String, Integer> selects){
        System.out.println("Выберите слова, которые стоит добавить в Черный список:\n" +
                "\"1\" - ВЫБРАТЬ\n" +
                "\"2\" - ПРОПУСТИТЬ\n" +
                "\"0\" - остановить (пропустить все остальные)\n");
        HashMap<String, Integer> r = new HashMap<>();
        Scanner scanner = new Scanner(System.in);
        int size = selects.size();
        int i = 1;
        for(Map.Entry<String, Integer> entry : selects.entrySet()) {
            String key = entry.getKey();
            System.out.print("(" + i + "/" + size + ") "+ key + ": " );
            switch (scanner.nextInt()){
                case 1:
                    r.put(entry.getKey(), entry.getValue());
                    break;
                case 0:
                    for(Map.Entry<String, Integer> e : r.entrySet())
                        selects.remove(e.getKey());
                    return r;
            }
            i++;
        }
        for(Map.Entry<String, Integer> entry : r.entrySet())
            selects.remove(entry.getKey());

        return r;
    }

    /**
     * Создать и вернуть экземпляр парной базы данных: основа слова + оригинальное слово из текста
     * @param str Строка как входные данные
     * @return экземпляр класса PairDatabase
     */
    public static PairDatabase createPairDatabase(String str) {//<T> HashMap<String, Integer>
        str = str.toLowerCase();
        Pattern reg = Pattern.compile("[a-zA-Zа-яА-Я]+");
        HashMap<String, Integer> list = new HashMap<String, Integer>();
        HashMap<String, String> list2 = new HashMap<String, String>();
        Matcher m = reg.matcher(str);
        while (m.find()) {
            if (m.group().length() > 2) {
                String wordbase = StemmerPorterRU.stem(m.group());
                if (!list.containsKey(wordbase)) {
                    list.put(wordbase, 1);
                    list2.put(wordbase, m.group());
                } else {
                    list.put(wordbase, list.get(wordbase) + 1);
                }
            }

        }

        return new PairDatabase(sortHashMapByValue(list), sortHashMapByValue(list2));
    }

    /**
     * Создать и установить экземпляр парной базы данных: основа слова + оригинальное слово из текста
     */
    public static void newPairDatabase(PairDatabase pairDatabase) {
        DATABASE = pairDatabase;
    }
    public static void newPairDatabase(String str) {
        DATABASE = createPairDatabase(str);
    }
    public static void clearPairDatabase(){
        DATABASE = new PairDatabase();
    }

    public static void readPairDatabase(){
        readPairDatabase(FILENAME_DATABASE);
    }
    public static void readPairDatabase(String fileName){
        PairDatabase pairDatabase = null;
        try {
            FileInputStream fileIn = new FileInputStream(fileName);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            pairDatabase = (PairDatabase) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException i) {
            i.printStackTrace();
        } catch (ClassNotFoundException c) {
            System.out.println("PairDatabase not found");
            c.printStackTrace();
        }
        expandPairDatabase(pairDatabase);
    }

    public static void expandPairDatabase(PairDatabase pairDatabase){
        for(Map.Entry<String, Integer> entry : pairDatabase.edit.entrySet())
            if (!DATABASE.edit.containsKey(entry.getKey())) {
                DATABASE.edit.put(entry.getKey(), entry.getValue());
                DATABASE.orig.put(entry.getKey(), pairDatabase.orig.get(entry.getKey()));
            }
            else if(DATABASE.edit.get(entry.getKey()) > 0 & entry.getValue() <= 0){
                DATABASE.edit.put(entry.getKey(), entry.getValue());
            }
    }

    public static void writePairDatabase(){
        writePairDatabase(FILENAME_DATABASE);
    }
    public static void writePairDatabase(String fileName){
        try {
            FileOutputStream fileOut =
                    new FileOutputStream(fileName);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(DATABASE);
            out.close();
            fileOut.close();
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    /**
     * Сортировка HashMap
     */
    private static <K, V extends Comparable<? super V>> HashMap<K, V> sortHashMapByValue(HashMap<K, V> map) {
        List<HashMap.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(HashMap.Entry.comparingByValue());

        HashMap<K, V> result = new LinkedHashMap<>();
        for (HashMap.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    //region TRASH
    /**
     * Расширение базы ключевых слов
     * @param string строка с обрабатываемыми словами
     * @throws IOException
     */
    public static void updateDataBase(String string) throws IOException {
        Pattern reg = Pattern.compile("[a-zA-Zа-яА-Я]+");
        Matcher m = reg.matcher(string);

        //region Comment
        System.out.println("[Расширение базы ключевых слов]\n" +
                        "// 1 - добавить основу\n" +
                        "// 2 - изменить и добавить\n" +
                        "// 0 - не добавлять\n");
        //endregion

        FileWriter writer = new FileWriter("output.txt", true);
        Scanner scanner = new Scanner(System.in);
        while (m.find()) {
            String wordbase = StemmerPorterRU.stem(m.group());
            System.out.print(m.group() + " (" + wordbase + ") -> ");
            int p = scanner.nextInt();
            switch (p) {
                case 2:
                    scanner.nextLine();
                    wordbase = scanner.nextLine();
                case 1:
                    writer.write(wordbase + System.getProperty("line.separator"));
                    break;
            }
        }
        writer.close();
    }
    //endregion
}
