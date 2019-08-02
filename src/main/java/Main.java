import StringMethods.StemmerPorterRU;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) throws IOException {
        load();
        console();
    }

    //region Variables

    private static String FILENAME_DATABASE_XLS = "Database.xls"; //файл таблицы Excel, хранит базу слов с которой работаем
    private static String FILENAME_DATABASE_SER = "Database.ser"; //сериализованный файл базы (не используется)
    private static Database DATABASE = new Database(); //экземпляр класса базы, хранит базу в программе
    private static long startTime = System.currentTimeMillis(); //время старта программы

    //endregion

    //region Database class

    /**
     * Класс базы данных для хранения слова, его повторений или идетификатора ключевого слова
     * (0 - ключевое, -1 - неключевое) и оригинала слова
     */
    static class Database implements Serializable {
        HashMap<String, Integer> edit = new HashMap<String, Integer>();
        HashMap<String, String> orig = new HashMap<String, String>();

        public HashMap<String, Integer> getEdit() {
            return edit;
        }

        public HashMap<String, String> getOrig() {
            return orig;
        }

        Database() {
        }

        Database(HashMap<String, Integer> edit, HashMap<String, String> orig) {
            this.edit = edit;
            this.orig = orig;
        }

        @Override
        public String toString() {
            return this.edit.toString() + "\n" + this.orig.toString();
        }
    }
    //endregion

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    //region Start methods

    //проверка существования файла таблицы, затем загрузка или создание файла таблицы
    private static void load() throws IOException {
        if(isFirstStart()){
            System.out.println("[!] Таблицы для работы программы необнаружено!");
            System.out.println("[i] Создание файла...");
            System.out.println("[i] Создание стобцов...");
            fromDatabaseToExcel();
            console_readDialog();
        }
        else
            fromExcelToDatabase();

    }
    //запуск консольного интерфейса
    private static void console() throws IOException {
        while(console_databaseOptions())
            console_databaseOptions();
        System.out.println("Время сессии (сек): " + (double) (System.currentTimeMillis() - startTime) / 1000);

    }
    /**
     * Проверка на первый запуск
     */
    private static boolean isFirstStart(){
        File f = new File(FILENAME_DATABASE_XLS);
        return !(f.exists() && !f.isDirectory());
    }

    //endregion

    //region Console commands and interface

    //меню команд для работы с базой
    private static boolean console_databaseOptions() throws IOException {
        Scanner console = new Scanner(System.in);
        System.out.println("\n*** УПРАВЛЕНИЕ БАЗОЙ\n");
        if(DATABASE.edit.size() == 0){
            System.out.println("{в базе нет слов}\n");
        }
        else {
            System.out.println("Текущая база в программе (" + DATABASE.edit.size()+ " слов): " + DATABASE.edit+"\n");
        }
        System.out.println("[Настройка базы]\n" +
                "\"1\" - Открыть таблицу\n" +
                "\"2\" - Обновить данные из таблицы\n" +
                "\"3\" - Сохранить данные в таблицу\n" +
                "\"4\" - Расширить базу в программе\n" +
                "\"5\" - Очистить базу в программе\n\n" +
                "[Отладка базы]\n" +
                "\"6\" - Ручная отладка\n" +
                "\"7\" - Автоматическая отладка\n\n" +
                "[Завершение работы]\n" +
                "\"0\" - Выход");
        while(true){
            System.out.print("> ");
            switch (console.nextInt()) {
                case 0:
                    return false;
                case 1:
                    openXLS();
                    return true;
                case 2:
                    fromExcelToDatabase();
                    return true;
                case 3:
                    fromDatabaseToExcel();
                    return true;
                case 4:
                    console_readDialog();
                    return true;
                case 5:
                    clearDatabase();
                    return true;
                case 6:
                    console_debugDatabase();
                    return true;
                case 7:
                    console_debugDatabaseByReps();
                    return true;
                default:
                    System.out.println("[!] Введеной команды не существует!");
                    break;
            }
        }
    }
    //меню для загрузки/создания базы из файла
    private static void console_readDialog() throws IOException {
        Scanner console = new Scanner(System.in);
        System.out.println("\n*** УСТАНОВКА БАЗЫ\n");
        System.out.println("\"1\" - Загрузить базу из таблицы Excel (.xls)\n" +
                "\"2\" - Создать базу из текстового файла (.txt)\n" +
                "\"0\" - Отмена");
        while(true){
            System.out.print("> ");
            switch (console.nextInt()){
                case 0:
                    return;
                case 1:
                    openReadDialog(false);
                    return;
                case 2:
                    openReadDialog(true);
                    return;
                default:
                    System.out.println("[!] Введеной команды не существует!");
                    break;
            }
        }
    }
    //меню ручной отладки
    private static void console_debugDatabase() {
        Scanner console = new Scanner(System.in);
        System.out.println("\n*** ОТЛАДКА БАЗЫ\n" +
                "База хранит все слова, которые поступали в программу.\n" +
                "В пункте \"Повторения\":\nключевые слова помечены \"0\";\n" +
                "неключевые имеют количество повторений со знаком минус (отрицательные значения);\n" +
                "необработанные слова имеют количество повторений.\n");
        System.out.println("\"1\" - Отладка в консоли автоматически найденных необработанных слов\n" +
                "\"2\" - Ручная отладка в таблице Excel\n" +
                "\"0\" - Отмена");
        while(true){
            System.out.print("> ");
            switch (console.nextInt()){
                case 0:
                    System.out.println("[i] Операция отменена");
                    return;
                case 1:
                    debugDatabase();
                    return;
                case 2:
                    openXLS();
                    return;
                default:
                    System.out.println("[!] Введеной команды не существует!");
                    break;
            }
        }
    }
    //меню автоматической отладки
    private static void console_debugDatabaseByReps(){
        Scanner console = new Scanner(System.in);
        System.out.println("\n*** АВТОМАТИЧЕСКАЯ ОТЛАДКА\n" +
                "Пометить ненужные слова до n повторений (включительно)\n");
        System.out.print("Введите значение N (\"0\" для отмены ): ");
        int reps = console.nextInt();
        if(reps == 0)
            System.out.println("[i] Операция отменена");
        else
            debugDatabaseByReps(reps);
    }

    //endregion

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    //region Create Database methods

    /**
     * Создать базу из текста из текстового файла
     */
    private static void createDatabaseFromTXT(String path) throws IOException {
        File f = new File(path);
        final int length = (int) f.length();

        String str = "";

        if (length != 0) {

            char[] cbuf = new char[length];

            InputStreamReader isr = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8);

            final int read = isr.read(cbuf);

            str = new String(cbuf, 0, read);
            isr.close();
        }

        setupDatabaseFromString(str);
    }
    /**
     * Создать и вернуть экземпляр парной базы данных: основа слова + оригинальное слово из текста
     *
     * @param str Строка как входные данные
     * @return экземпляр класса Database
     */
    private static Database createDatabaseFromString(String str) {//<T> HashMap<String, Integer>
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

        return new Database(sortHashMapByValue(list), sortHashMapByValue(list2));
    }
    /**
     * Создать и установить экземпляр парной базы данных: основа слова + оригинальное слово из текста
     */
    private static void setupDatabase(Database database) {
        expandDatabase(database);
    }
    private static void setupDatabaseFromString(String str) {
        expandDatabase(createDatabaseFromString(str));
    }

    //endregion

    //region Change Database methods

    /**
     * Добавление слов в базу (только тех, которые не встречаются или обработаны в новой базе)
     *
     */
    private static void expandDatabase(Database database) {
        for (Map.Entry<String, Integer> entry : database.edit.entrySet())
            if (!DATABASE.edit.containsKey(entry.getKey())) {
                DATABASE.edit.put(entry.getKey(), entry.getValue());
                DATABASE.orig.put(entry.getKey(), database.orig.get(entry.getKey()));
            } else if (DATABASE.edit.get(entry.getKey()) > 0 & entry.getValue() <= 0) {
                DATABASE.edit.put(entry.getKey(), entry.getValue());
            }
    }
    /**
     * Очистка базы даных внутри программы
     */
    private static void clearDatabase() {
        DATABASE.edit.clear();
        DATABASE.orig.clear();
        System.out.println("[i] База из программы была очищена. Исходный файл таблицы не затронут");
    }
    /**
     * Запуск отладки ключевых слов в базе
     */
    private static void debugDatabase() {
        System.out.println("\n*** НАЧАЛО ОТЛАДКИ БАЗЫ\n");
        int quantityAllWords = DATABASE.edit.size();
        int quantityPendingWords = 0;
        for (Map.Entry<String, Integer> entry : DATABASE.edit.entrySet())
            if (entry.getValue() > 0)
                quantityPendingWords++;
        if (quantityPendingWords == 0) {
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
        for (Map.Entry<String, Integer> entry : DATABASE.edit.entrySet()) {
            if (entry.getValue() > 0) {
                System.out.print("(" + i + "/" + quantityPendingWords + ") " + entry.getKey() + " (" + DATABASE.orig.get(entry.getKey()) + "): ");
                switch (scanner.nextInt()) {
                    case 1:
                        entry.setValue(0);
                        break;
                    case 2:
                        entry.setValue(-1 * entry.getValue());
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
    private static void debugDatabaseByReps(int reps) {
        if (reps < 1) {
            System.out.println("[!] Неверное значение");
            return;
        }
        for (Map.Entry<String, Integer> entry : DATABASE.edit.entrySet()) {
            if (entry.getValue() <= reps & entry.getValue() > 0) {
                entry.setValue(-1 * entry.getValue());
            }
        }
        System.out.println("[i] Слова помечены");
    }
    /**
     * Открыть файл таблицы
     */
    private static void openXLS(){
        openXLS(FILENAME_DATABASE_XLS);
    }
    private static void openXLS(String path) {
        Desktop desktop = null;
        if (Desktop.isDesktopSupported()) {
            desktop = Desktop.getDesktop();
        }
        try {
            File file = new File(path);
            if (!file.exists()) {
                System.out.println("[!] Таблицы для работы программы необнаружено!");
                System.out.println("[i] Создание файла...");
                System.out.println("[i] Создание стобцов...");
                fromDatabaseToExcel();
            }
            System.out.println("[i] Открытие таблицы: " + file.getAbsolutePath());
            assert desktop != null;
            desktop.open(file);
        } catch (Exception ignored) {
        }
    }

    //endregion

    //region RW methods (Read/Write)

    /**
     * Сохранить базу из программы в таблицу
     */
    private static void fromDatabaseToExcel() throws IOException {
        fromDatabaseToExcel(DATABASE);
    }
    private static void fromDatabaseToExcel(Database database) throws IOException {
        //region Прогресс бар
        /*
        final JFrame frame = new JFrame("Сохранение таблицы");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        int BOR = 10;
        panel.setBorder(BorderFactory.createEmptyBorder(BOR, BOR, BOR, BOR));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(Box.createVerticalGlue());

        JLabel label = new JLabel();
        //progressBar1.setIndeterminate(true);
        label.setText("");
        panel.add(label);

        panel.add(Box.createVerticalGlue());

        final JProgressBar progressBar2 = new JProgressBar();
        progressBar2.setStringPainted(true);
        progressBar2.setMinimum(0);
        progressBar2.setMaximum(100);
        panel.add(progressBar2);

        panel.add(Box.createVerticalGlue());

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(panel, BorderLayout.CENTER);

        frame.setPreferredSize(new Dimension(500, 225));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        */
        //endregion
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet("Result");

        int rownum = 0;
        Cell cell;
        Row row;

        HSSFCellStyle style = createStyleForTitle(workbook);

        row = sheet.createRow(rownum);

        cell = row.createCell (0, CellType.FORMULA);
        String formula = "\"Слов: \" & COUNT(C:C)";
        cell.setCellFormula(formula);
        cell.setCellStyle(style);

        cell = row.createCell(1, CellType.STRING);
        cell.setCellValue("Оригинал");
        cell.setCellStyle(style);

        cell = row.createCell(2, CellType.STRING);
        cell.setCellValue("Повторения");
        cell.setCellStyle(style);

        //int i = 1;
        for (Map.Entry<String, Integer> entry : DATABASE.edit.entrySet()) {
            //label.setText("("+i+" / "+DATABASE.edit.size()+") "+entry.getKey());
            //i++;
            //progressBar2.setValue((int)((double)100/DATABASE.edit.size()*i));
            rownum++;
            row = sheet.createRow(rownum);

            cell = row.createCell(0, CellType.STRING);
            cell.setCellValue(entry.getKey());

            cell = row.createCell(1, CellType.STRING);
            cell.setCellValue(DATABASE.orig.get(entry.getKey()));

            cell = row.createCell(2, CellType.NUMERIC);
            cell.setCellValue(entry.getValue());
        }

        sheet.setColumnWidth(0, 10000);
        sheet.setColumnWidth(1, 10000);
        sheet.setColumnWidth(2, 3000);

        File file = new File(FILENAME_DATABASE_XLS);

        //frame.hide();
        FileOutputStream outFile = new FileOutputStream(file);
        workbook.write(outFile);
        System.out.println("[i] Сохранение в таблицу прошло успешно: " + file.getAbsolutePath());
        workbook.close();
        outFile.close();

    }

    /**
     * Загрузить базу из таблицы в программу
     */
    private static void fromExcelToDatabase() throws IOException {
        fromExcelToDatabase(FILENAME_DATABASE_XLS);
    }
    private static void fromExcelToDatabase(String str) throws IOException {
        // Read XSL file
        FileInputStream inputStream = new FileInputStream(new File(str));

        // Get the workbook instance for XLS file
        HSSFWorkbook workbook = new HSSFWorkbook(inputStream);

        // Get first sheet from the workbook
        HSSFSheet sheet = workbook.getSheetAt(0);

        // Get iterator to all the rows in current sheet
        Iterator<Row> rowIterator = sheet.iterator();

        HashMap<String, Integer> list = new HashMap<String, Integer>();
        HashMap<String, String> list2 = new HashMap<String, String>();

        if(rowIterator.hasNext())
            rowIterator.next();

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            Iterator<Cell> cellIterator = row.cellIterator();

            Cell cell = cellIterator.next();
            String edit = cell.getStringCellValue();
            cell = cellIterator.next();
            String orig = cell.getStringCellValue();
            cell = cellIterator.next();
            int reps = (int)cell.getNumericCellValue();

            if (edit.length() > 2) {
                if (!DATABASE.edit.containsKey(edit)) {
                    DATABASE.edit.put(edit, reps);
                    DATABASE.orig.put(edit, orig);
                }
                else{
                    DATABASE.edit.put(edit, DATABASE.edit.get(edit) + reps);
                }
            }
        }

        inputStream.close();

        File file = new File(FILENAME_DATABASE_XLS);
        System.out.println("[i] База загружена из таблицы успешно: " + file.getAbsolutePath());
    }
    private static void openReadDialog(boolean isCreateFromTXT) throws IOException {
        JFileChooser chooser = new JFileChooser() {

            @Override
            protected JDialog createDialog(Component parent)
                    throws HeadlessException {
                JDialog dialog = super.createDialog(parent);
                // config here as needed - just to see a difference
                dialog.setLocationByPlatform(true);
                // might help - can't know because I can't reproduce the problem
                dialog.setAlwaysOnTop(true);
                return dialog;
            }

        };
        chooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter filter;
        String label = "";
        if (isCreateFromTXT) {
            chooser.addChoosableFileFilter(new FileNameExtensionFilter("Текстовый файл (.txt)", "txt"));
            label = "Создать базу из текстового файла (.txt)";
        } else {
            chooser.addChoosableFileFilter(new FileNameExtensionFilter("Таблица Excel (.xls)", "xls"));
            label = "Загрузить базу из Excel файла (.xls)";

        }
        int ret = chooser.showDialog(null, label);
        if (ret == 0) {
            if (isCreateFromTXT) {
                createDatabaseFromTXT(chooser.getSelectedFile().getPath());
            } else {
                fromExcelToDatabase(chooser.getSelectedFile().getPath());
            }
        }
    }

    //endregion

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    //region Other methods

    /**
     * Стиль для колонок таблицы Excel
     */
    private static HSSFCellStyle createStyleForTitle(HSSFWorkbook workbook) {
        HSSFFont font = workbook.createFont();
        HSSFCellStyle style = workbook.createCellStyle();
        font.setBold(true);
        style.setFont(font);
        return style;
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

    //region Backup methods for Database SER
    /**
     * Чтение базы из файла
     */
    private static void readDatabaseFromSER() {
        readDatabaseFromSER(FILENAME_DATABASE_SER);
    }
    private static void readDatabaseFromSER(String fileName) {
        Database database = new Database();
        try {
            FileInputStream fileIn = new FileInputStream(fileName);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            database = (Database) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException i) {
            i.printStackTrace();
        } catch (ClassNotFoundException c) {
            System.out.println("Database not found");
            c.printStackTrace();
        }
        expandDatabase(database);
    }

    /**
     * Запись базы в файл
     */
    private static void writeDatabaseToSER() {
        writeDatabaseToSER(FILENAME_DATABASE_SER);
    }
    private static void writeDatabaseToSER(String fileName) {
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
    //endregion

    //endregion
}
//666? Совпадение? Не думаю...