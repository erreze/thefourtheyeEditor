package thefourtheyeEditor;
import java.awt.BorderLayout;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import thefourtheyeEditor.supportedLanguages.CPP;
import thefourtheyeEditor.supportedLanguages.CSharp;
import thefourtheyeEditor.supportedLanguages.JAVA;
import thefourtheyeEditor.supportedLanguages.Python;
import thefourtheyeEditor.supportedLanguages.VB;

import com.topcoder.client.contestant.ProblemComponentModel;
import com.topcoder.shared.language.Language;
import com.topcoder.shared.problem.Renderer;

public class Main
{
   private JPanel    minimalPanel;
   private File      solutionFileObject = null;
   private static JTextArea minimalEditor;
   public  static Properties globalProperties = null;
   public  static File configFile = new File(System.getProperty("user.home"),
         "contestapplet.conf");
   
   public static String getConfigValue(String key) throws Exception
   {
      if (globalProperties == null)
      {
         globalProperties = new Properties();
         globalProperties.load(new FileInputStream(configFile));
      }
      String Result = globalProperties.getProperty("thefourtheyeEditor." + key);
      if (Result == null)
      {
         updateStatus("Configuration Entry : thefourtheyeEditor." + key 
               + " is not found in " + configFile.getAbsolutePath(), true);
         return "";
      }
      return Result;
   }

   public Main()
   {
      minimalEditor = new JTextArea();
      minimalEditor.setForeground(Color.white);
      minimalEditor.setBackground(Color.black);
      minimalEditor.setEditable(false);

      minimalPanel = new JPanel(new BorderLayout());
      minimalPanel.add(new JScrollPane(minimalEditor), BorderLayout.CENTER);
   }

   public JPanel getEditorPanel()
   {
      return minimalPanel;
   }

   private static void updateStatus(String statusMessage, boolean isError)
   {
      updateStatus ((isError?"[ERROR] ":"") + statusMessage);
   }

   private static void updateStatus(String statusMessage)
   {
      if (statusMessage == "")
      {
         minimalEditor.setText("");
      }
      else
      {
         minimalEditor.setText(minimalEditor.getText() + statusMessage + "\n");
      }
   }

   public void setProblemComponent (ProblemComponentModel component, 
         Language language, Renderer renderer)throws Exception
   {
      String contestName = component.getProblem().getRound().getContestName();
      boolean isPracticeRound = component.getProblem().getRound().getRoundType()
            .isPracticeRound();
      String contestType = isPracticeRound ? "Practice" : "Match";
      String problemName = component.getProblem().getName();
      File contestDirFileObject = new File(new 
            File(getConfigValue("SolutionsDirectory"), contestType),
            contestName);

      if (contestDirFileObject.exists() == false)
      {
         if (contestDirFileObject.mkdirs() == false)
         {
            updateStatus("Directory creation failed : " + 
                  contestDirFileObject.getAbsolutePath(), true);
            return;
         }
      }
      else if (contestDirFileObject.isDirectory() == false)
      {
         updateStatus("Directory creation failed : " + 
               contestDirFileObject.getAbsolutePath() + ". There is a " + 
               "file with the same name exists", true);
         return;
      }
      else
      {
         if (contestDirFileObject.canWrite() == false)
         {
            updateStatus("Directory exists : " + 
                  contestDirFileObject.getAbsolutePath() + ", but NOT WRITABLE");
         }
      }

      File problemFileObject = 
            new File (contestDirFileObject, problemName + ".html");
      BufferedWriter writeFile = 
            new BufferedWriter(new FileWriter(problemFileObject));
      writeFile.write(renderer.toHTML(language));
      writeFile.close();
      updateStatus("Wrote problem statement to : " + 
            problemFileObject.getAbsolutePath());

      try
      {
         LanguageInterface currentLanguage = getLanguage(language, component);
      
         solutionFileObject = new File (contestDirFileObject, 
           currentLanguage.getClassName() + currentLanguage.getFileExtension());
   
         if (getConfigValue("replaceSolutionIfAlreadyExists").equals("yes") ||
               solutionFileObject.exists() == false)
         {
            BufferedWriter solutionWriter = 
                  new BufferedWriter(new FileWriter(solutionFileObject));
            ArrayList<String> testSuit = currentLanguage.getSolutionTemplate();
            for (String str : testSuit)
            {
               solutionWriter.write(str + "\n");
            }
            solutionWriter.close();
            updateStatus("Wrote solution template to " + 
                  solutionFileObject.getAbsolutePath());
         }
         else
         {
            updateStatus("NOT overwriting solution : " + 
                  solutionFileObject.getAbsolutePath());
         }
      }
      catch (Exception ex)
      {
         updateStatus(ex.getMessage(), true);
      }

   }

   private LanguageInterface getLanguage(Language language, 
         ProblemComponentModel component) throws Exception
   {
      switch(language.getName())
      {
         case "C++"    : return new CPP    (language, component);
         case "Java"   : return new JAVA   (language, component);
         case "C#"     : return new CSharp (language, component);
         case "VB"     : return new VB     (language, component);
         case "Python" : return new Python (language, component);
         default       : return null;
      }
   }

   public String getSource() throws Exception
   {
      BufferedReader solutionReader = 
            new BufferedReader(new FileReader(solutionFileObject));
      String readLine = "", solution = "";
      boolean insideCutSection = false;
      while ((readLine = solutionReader.readLine()) != null)
      {
         if (readLine.contains("//BEGINCUT") || readLine.contains("#BEGINCUT")
               || readLine.contains("'BEGINCUT"))
         {
            insideCutSection = true;
         }
         if (insideCutSection == false)
         {
            solution += readLine + "\n";
         }
         if (readLine.contains("//ENDCUT") || readLine.contains("#ENDCUT")
               || readLine.contains("'ENDCUT"))
         {
            insideCutSection = false;
         }
      }
      solutionReader.close();
      return solution;
   }

   public void setSource(String source){}
}
