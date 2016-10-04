package sample;

import com.sun.org.apache.regexp.internal.RE;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Controller implements Initializable{
    @FXML
    CodeArea codeArea;
    @FXML
    TextArea txtMensajes;
    @FXML
    ListView lsErrores;

    private static final String[] KEYWORDS = new String[] {
            "abstract", "assert", "boolean", "break", "byte",
            "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else",
            "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import",
            "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while"
    };

    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String PAREN_PATTERN = "\\(|\\)";
    private static final String BRACE_PATTERN = "\\{|\\}";
    private static final String BRACKET_PATTERN = "\\[|\\]";
    private static final String SEMICOLON_PATTERN = "\\;";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
    private static final String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<PAREN>" + PAREN_PATTERN + ")"
                    + "|(?<BRACE>" + BRACE_PATTERN + ")"
                    + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
                    + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
    );

    public void initialize(URL location, ResourceBundle resources){
        colorPalabras_Reservadas();
        contadorLineas();
        inicializaErrores_Prueba();
        palabrasLexico();
    }

    File archivo;

    public void cargarArchivo() throws IOException{
        FileChooser fileChooser = new FileChooser();            //Nos deja seleccionar un archivo a cargar
        fileChooser.setTitle("Seleccione un archivo para cargar");
        archivo = fileChooser.showOpenDialog(new Stage());//Obtenemos ruta

        if(archivo != null) {
            FileInputStream stream = new FileInputStream(archivo);
            //Declaramos el encoding de entrada
            BufferedReader entrada = new BufferedReader(new InputStreamReader(stream, Charset.forName("UTF-8")));

            String linea = null;
            while ((linea = entrada.readLine()) != null) {//Lee archivo y lo pone en el textArea
                codeArea.appendText(linea + "\n");
            }
            entrada.close();

            //txtArea.appendText(rutaArchivo);
            txtMensajes.setText("");
            txtMensajes.setText("Archivo cargado con éxito");
        }
        else {
            txtMensajes.setText("");
            txtMensajes.setText("Error de carga en archivo");
        }

        /*
        codeArea.appendText("Hola\n");
        codeArea.appendText("Mundo\n");
        codeArea.appendText("Gitano priemra segunda tercera cuarta");
        */
    }

    String contenidoArchivo = new String();
    public void guardarArchivo () throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar archivo");

        //Extensión
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
        FileChooser.ExtensionFilter extFilter2 = new FileChooser.ExtensionFilter("RTF files (*.rtf)", "*.rtf");
        fileChooser.getExtensionFilters().addAll(extFilter,extFilter2);

        //Muestra dialogo
        archivo = fileChooser.showSaveDialog(new Stage());

        contenidoArchivo = (String) codeArea.getText();//Obtenemos contenido del textArea y la pasamosa string

        if(archivo != null) {
            SaveFile(contenidoArchivo,archivo);
            txtMensajes.setText("");
            txtMensajes.setText("Archivo guardado con éxito");
        }
        else {
            txtMensajes.setText("");
            txtMensajes.setText("Error en guardado de archivo");
        }

    }

    private void SaveFile(String content, File file){
        try {
            FileWriter fileWriter = null;

            fileWriter = new FileWriter(file);
            fileWriter.write(content);
            fileWriter.close();
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void guardarRapido () { // Guarda el archivo si previamente fue guardado como. No pregunta
        if(contenidoArchivo.equals("")) {
            txtMensajes.setText("");
            txtMensajes.setText("Primero ocupa guardar como");
        }
        else {
            String contenido = (String) codeArea.getText();
            SaveFile(contenido, archivo);
            txtMensajes.setText("");
            txtMensajes.setText("Archivo guardado rápido con éxito");
        }
    }

    public void contadorLineas(){
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
    }

    public void colorPalabras_Reservadas(){
        codeArea.richChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved())) // XXX
                .subscribe(change -> {
                    codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText()));
                });
    }

    private static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder
                = new StyleSpansBuilder<>();
        while(matcher.find()) {
            String styleClass =
                    matcher.group("KEYWORD") != null ? "keyword" :
                            matcher.group("PAREN") != null ? "paren" :
                                    matcher.group("BRACE") != null ? "brace" :
                                            matcher.group("BRACKET") != null ? "bracket" :
                                                    matcher.group("SEMICOLON") != null ? "semicolon" :
                                                            matcher.group("STRING") != null ? "string" :
                                                                    matcher.group("COMMENT") != null ? "comment" :
                                                                            null; /* never happens */ assert styleClass != null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    public void inicializaErrores_Prueba(){
        ObservableList<Integer> items = FXCollections.observableArrayList(1,2,3,4);
        lsErrores.setItems(items);
    }

    public void manejoErrores(){
        int valor = lsErrores.getSelectionModel().getSelectedIndex();
        //Marcado de linea
        codeArea.moveTo(valor,0);//Primer digito comenzando de 0 indica linea, segundo indica columna
        codeArea.selectLine();//Resalta toda la linea
        //System.out.println(codeArea.getText(2));//Obtenemos el texto de la linea 3 con el getText. SOLO PRUEBA
    }

    public void abrirLexico() throws IOException{
        Stage primaryStage=new Stage();
        Parent root = FXMLLoader.load(getClass().getResource("Lexico.fxml"));
        root.getStylesheets().add(Controller.class.getResource("java-keywords.css").toExternalForm());//Importamos el css a usar
        primaryStage.setTitle("Compilador Manual - Traductores II");
        primaryStage.setScene(new Scene(root, 600, 400));
        primaryStage.show();

        generarLexico();
        //llenarTablas_Lexico();
        pruebasListas();

    }

    public static List<Integer> lexico_ID = new ArrayList<Integer>();
    public static List<Integer> lexico_Linea = new ArrayList<Integer>();
    public static List<String> lexico_Token = new ArrayList<String>();
    public static List<String> lexico_Identificador = new ArrayList<String>();




    public void generarLexico(){//Enviar el token y que busque en método

        lexico_ID.clear();
        lexico_Linea.clear();
        lexico_Token.clear();
        lexico_Identificador.clear();

        String patternString_R = "\\b(" + String.join("|",palabrasReservadas) + ")\\b";
        Pattern patternReservadas = Pattern.compile(patternString_R);

        String patternString_M = "\\b(" + String.join("|",operadoresMatematicos) + ")\\b";
        //Pattern patternMatematicos = Pattern.compile(patternString_M);

        String patternString_C = "\\b(" + String.join("|",Compara) + ")\\b";
        //Pattern patternComparador = Pattern.compile(patternString_C);

        String patternString_S = "\\b(" + String.join("|",Separador) + ")\\b";
        //Pattern patternSeparador = Pattern.compile(patternString_S);

        String patternString_E = "\\b(" + String.join("|","([0-9]*)") + ")\\b";
        //Pattern patternEntero = Pattern.compile(patternString_E);

        String patternString_Cad = "\\b(" + String.join("|","([\"][a-zA-Z][\"]*)") + ")\\b";
        //Pattern patternCadena = Pattern.compile(patternString_Cad);


        String prueba="";
        int contadorID=1;
        int contadorLinea=1;
        int g=codeArea.getLength();
        String[] lineasCodeArea=codeArea.getText().split("\\n");
        System.out.println("Length: "+g);

        /*
        String palabras = ("(programa|var|inicio|fin|byte|entero|largo|flotante|bool|doble|caracter|cadena|mod|libreria|o|y|no|verdad|falso|seleccion|si|sino|evalua|por_omision|finsel|final|finhazlo|hazlo_si|repite|finrepite|como|para|finpara|modo|finfunc|funcion|procedimiento|finproc|seccion)" +
                "|([+|-|/|*|**]+)" +
                "|([<|<=|>|>=|==|<>]+)" +
                "|(;)" +
                "|([0-9]*)" +
                "|([a-zA-Z]+)" +
                "|(=)");
        */

        String Reserv = "(programa|var|inicio|fin|byte|entero|largo|flotante|bool|doble|caracter|cadena|mod|libreria|no|y|o|verdad" +
                "falso|seleccoion|si|finpara|modo|finfuno|funcion|procedimiento|finproc|seccion|sino|evalua|por_omisions|finsel|final" +
                "|hinhazlo|hazlo_si|repite|finrepite|como|para)";

        String comp = (".*(<|>|>=|<=|==|<>).*");
        String numero = ("([0-9]+)");
        String sep = (".*(;).*");
        String mat = (".*([+|-|/|*|**|mod]).*");
        String operador = "([*][*]|[+]|-|[*]|mod)";
        String car = ("([a-zA-Z]+)");
        String asig = "(=)";
        String cadena = "([\"]\\w*\\s*[\"])";
        String cadenaCara = "([\"].*[\"])";
        String tipoDato = "(bool|entero|largo|byte)";
        String operadorLogico = "(y|o)";
        String operadorNegacion = "(no)";
        String Identificador = "([A-Z]{1,1}[a-zA-Z0-9]{2,254})";
        String detectarCadena = "(\")";

        int contarComilla=0;

        //Pattern p = Pattern.compile(Reserv+"|"+cadenaCara+"|"+sep+"|"+operador+"|"+numero+"|"+comp+"|"+asig);

        String juntaCadena="";
        String juntaComentario_Linea="";
        String juntaComentario_Bloque="";
        String numeroFlotante="";

        boolean comentarioBloque_Encontrado=false;

        for(int i=0;i<lineasCodeArea.length;i++) {
            prueba="";
            codeArea.moveTo(i, 0);
            codeArea.selectLine();
            prueba=(String)codeArea.getSelectedText();
            /*
            Matcher m =p.matcher(prueba);
            while (m.find()){
                if(m.group(1)!=null){
                    System.out.println("Reservada: "+m.group(1));
                }
                if(m.group(2)!=null){
                    System.out.println("Cadena: "+m.group(2));
                }
                if(m.group(3)!=null){
                    System.out.println("Separador: "+m.group(3));
                }
                if(m.group(4)!=null){
                    System.out.println("Operador: "+m.group(4));
                }
                if(m.group(5)!=null){
                    System.out.println("Numero: "+m.group(5));
                }
                if(m.group(6)!=null){
                    System.out.println("Comparador: "+m.group(6));
                }
                if(m.group(7)!=null){
                    System.out.println("Asignacion: "+m.group(7));
                }
            }
            */


            String[] sinEspacios = prueba.split("(\\b)|(?<=;)|(?=;)|(?<=//})|(?=//})");
            juntaCadena="";
            juntaComentario_Linea="";
            numeroFlotante="";
            boolean cadenaEncontrada=false;
            boolean comentarioLinea_Encontrado=false;
            for(int j=0;j<sinEspacios.length;j++){
                System.out.println("sinEspacios: "+sinEspacios[j]);
                if(sinEspacios[j].matches(comp) && cadenaEncontrada==false && comentarioLinea_Encontrado==false && comentarioBloque_Encontrado==false){
                    System.out.println("Comparador: "+sinEspacios[j]);
                }
                else if(sinEspacios[j].matches(operador) && cadenaEncontrada==false && comentarioLinea_Encontrado==false && comentarioBloque_Encontrado==false){
                    System.out.println("Operadores: "+sinEspacios[j]);
                }
                if(sinEspacios[j].matches(patternString_R) && cadenaEncontrada==false && comentarioLinea_Encontrado==false && comentarioBloque_Encontrado==false){
                    System.out.println("Reservada: "+sinEspacios[j]);
                }
                if(sinEspacios[j].matches(tipoDato) && cadenaEncontrada==false && comentarioLinea_Encontrado==false && comentarioBloque_Encontrado==false){
                    System.out.println("Tipo Dato: "+sinEspacios[j]);
                }
                if(sinEspacios[j].matches(operadorLogico) && cadenaEncontrada==false && comentarioLinea_Encontrado==false && comentarioBloque_Encontrado==false){
                    System.out.println("Operador Lógico: "+sinEspacios[j]);
                }
                if(sinEspacios[j].matches(operadorNegacion) && cadenaEncontrada==false && comentarioLinea_Encontrado==false && comentarioBloque_Encontrado==false){
                    System.out.println("Operador Negación: "+sinEspacios[j]);
                }
                if(sinEspacios[j].matches("\"") || cadenaEncontrada==true  && comentarioLinea_Encontrado==false && comentarioBloque_Encontrado==false){
                    cadenaEncontrada=true;
                    juntaCadena+=sinEspacios[j];
                    if(sinEspacios[j].equals("\""))
                        contarComilla++;
                    if(contarComilla==2) {
                        cadenaEncontrada = false;
                        System.out.println("Cadena: "+juntaCadena);
                    }
                }
                if(sinEspacios[j].matches("(\\{)") || comentarioLinea_Encontrado==true && cadenaEncontrada==false && comentarioBloque_Encontrado==false){
                    System.out.println("Entró");
                    comentarioLinea_Encontrado=true;
                    juntaComentario_Linea+=sinEspacios[j];
                    System.out.println("Junta Linea: "+juntaComentario_Linea);
                    if(sinEspacios[j].equals("}")) {
                        comentarioLinea_Encontrado=false;
                        System.out.println("Comentario Linea: "+juntaComentario_Linea);
                    }
                }
                if(sinEspacios[j].matches("(\\{//)") || comentarioBloque_Encontrado==true && comentarioLinea_Encontrado==false && cadenaEncontrada==false){
                    System.out.println("Entró");
                    comentarioBloque_Encontrado=true;
                    juntaComentario_Bloque+=sinEspacios[j];
                    System.out.println("Junta Bloque: "+juntaComentario_Bloque);
                    if(sinEspacios[j].matches("(//})")) {
                        //System.out.println("holi");
                        comentarioBloque_Encontrado=false;
                        System.out.println("Comentario Bloque: "+juntaComentario_Bloque);
                        juntaComentario_Bloque="";
                    }
                }
                if(sinEspacios[j].matches(sep) && cadenaEncontrada==false  && comentarioLinea_Encontrado==false && comentarioBloque_Encontrado==false){
                    System.out.println("Separador: "+sinEspacios[j]);
                }
                if(sinEspacios[j].matches(numero) && cadenaEncontrada==false  && comentarioLinea_Encontrado==false && comentarioBloque_Encontrado==false){
                    int valor1=0, valor2=0;
                    valor1=j+1;
                    valor2=j+2;
                    if(valor1<sinEspacios.length){
                        if(sinEspacios[j+1].equals(".")){
                            if(valor2<sinEspacios.length){
                                if (sinEspacios[j + 2].matches("([0-9]+)")) {
                                    numeroFlotante = sinEspacios[j] + sinEspacios[j + 1] + sinEspacios[j + 2];
                                    System.out.println("Flotante: " + numeroFlotante);
                                    j = j + 2;
                                }
                            }
                        }
                    }
                    else
                        System.out.println("Entero: "+sinEspacios[j]);
                }
                if(sinEspacios[j].matches(asig) && cadenaEncontrada==false  && comentarioLinea_Encontrado==false && comentarioBloque_Encontrado==false){
                    System.out.println("Asignación: "+sinEspacios[j]);
                }
                if(sinEspacios[j].matches(Identificador) && cadenaEncontrada==false  && comentarioLinea_Encontrado==false && comentarioBloque_Encontrado==false){
                    System.out.println("Identificador: "+sinEspacios[j]);
                }
            }

            contadorLinea++;
            System.out.println("Datos de la linea "+(i+1)+" : "+ prueba);
        }
    }

    List<String> palabrasReservadas = new ArrayList<String>();
    List<String> operadoresMatematicos = new ArrayList<String>();
    List<String> Compara = new ArrayList<String>();
    List<String> Separador = new ArrayList<String>();



    public void palabrasLexico(){//Poblamos las lista para verificar las palabras
        ObservableList<String> Reservadas = FXCollections.observableArrayList("programa","var","inicio","fin","flotante","doble","caracter","cadena","mod","libreria","verdad","falso","seleccion","si"
                ,"sino","evalua","por_omision","finsel","final","finhazlo","hazlo_si","repite","finrepite","como","para"
                ,"finpara","modo","finfunc","funcion","procedimiento","finproc","seccion"
        );

        ObservableList<String> Matematicos = FXCollections.observableArrayList("+","-","/","*","**");

        ObservableList<String> Comparadores = FXCollections.observableArrayList(">",">=","<","<=","==","<>");

        ObservableList<String> Separadores = FXCollections.observableArrayList(";");

        palabrasReservadas.addAll(Reservadas);
        operadoresMatematicos.addAll(Matematicos);
        Compara.addAll(Comparadores);
        Separador.addAll(Separadores);
    }

    /*
    public void llenarTablas_Lexico(){
        for (int i=0;i<lexico_ID.size();i++)
            txtID.appendText(Integer.toString(lexico_ID.get(i)));
        for (int i=0;i<lexico_Linea.size();i++)
            txtLinea.appendText(Integer.toString(lexico_Linea.get(i)));
        for (int i=0;i<lexico_Token.size();i++)
            txtToken.appendText(lexico_Token.get(i));
        for (int i=0;i<lexico_Identificador.size();i++)
            txtIdentificador.appendText(lexico_Identificador.get(i));
    }
    */

    public void pruebasListas(){
        for (int i=0;i<lexico_ID.size();i++)
            System.out.println("ID: "+lexico_ID.get(i));
        for (int i=0;i<lexico_Linea.size();i++)
            System.out.println("Linea: "+lexico_Linea.get(i));
        for (int i=0;i<lexico_Token.size();i++)
            System.out.println("Token: "+lexico_Token.get(i));
        for (int i=0;i<lexico_Identificador.size();i++)
            System.out.println("Identificador: "+lexico_Identificador.get(i));
    }
}
