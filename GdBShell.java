import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import static cTools.KernelWrapper.*;


public class GdBShell {

    public static void main(String[] args) {

        System.out.println("Welcome!");
        String shellPrefix = "EN@GdBShell:";//print actual user instead of EN??
        int runningInt = 0;
        Scanner scn = new Scanner(System.in);
        while (true) {

            parseInput(getInput(runningInt, shellPrefix, scn));
            runningInt++;
        }


    }

    static String[] getInput(int runningInt, String shellPrefix, Scanner scn) {


        String directoryPath = System.getProperty("user.dir");
        String[] directoryArray = directoryPath.split("/");
        String str = "~";


        //gets rid of the home/user directory and builds a string
        for (int i = 3; (i < directoryArray.length); i++) {
            str = str + "/" + directoryArray[i];
        }
        System.out.print("(" + runningInt + ")");
        printColor(shellPrefix, "purple", false);
        printColor(str + "$ ", "cyan", false);

        String input = scn.nextLine();//get input

        String[] inputArray = input.split("\\s+");//split at whitespace characters such as tabs and spaces


        return inputArray;

    }

    static void parseInput(String[] inputArray) {

        //check for empty inputs ?


        //implements the exit terminal function
        if (inputArray[0].equals("exit")) {
            if (inputArray.length == 1) {
                printColor("Session ended by user", "red", true);
                System.exit(0);
            } else {
                printColor("\"exit\" doesn't take parameters. It will end this current shell. Did you try to find a different executable?", "red", true);
                return;
            }

        }


        String[][] commandsArr = splitArray(inputArray, "&&");


        int prevValue;//saves the return code of the previously executed command


        //execute single commands here

        prevValue = parseRedirect(commandsArr[0]);
        int i;//allows to save the command position, one command already executed


        boolean singleCommand = true;
        //execute possible concatenations
        for (i = 1; i < commandsArr.length; i++) {


            singleCommand = false;
            //execute if previous command was successful, else do error printing


            if (prevValue == 0) {

                prevValue = parseRedirect(commandsArr[i]);
            } else {
                break;
            }
        }

        if (prevValue != 0 && singleCommand == false) {
            printColor("Stopped concatenation at command number " + (i - 1) + " \"" + String.join(" ", commandsArr[i - 1]) + "\"", "red", true);
        }


        return;
    }

    static int parseRedirect(String[] inputArray) {//should take pipe and read/write,, add >> for append?
        //find last occurrences of < and >
        int redirectInPos = -1;
        int redirectOutPos = -1;
        boolean setFirstBool = true;
        int firstPos = inputArray.length;
        for (int i = 0; i < inputArray.length; i++) {
            if (inputArray[i].equals("<")) {
                redirectInPos = i;
                if (setFirstBool) {
                    firstPos = i;
                    setFirstBool = false;
                }
            }
            if (inputArray[i].equals(">")) {
                redirectOutPos = i;
                if (setFirstBool) {
                    firstPos = i;
                    setFirstBool = false;
                }
            }
            /*if(inputArray[i].equals(">>")){
                appendOutPos = i;
            }*/
        }


        //TODO look up error return value of OPEN to separate from no redirect initialized as -1
        int fd_in = -1;
        if (redirectInPos != -1) {
            if (inputArray.length > redirectInPos + 1 && checkls(inputArray[redirectInPos + 1])) {
                fd_in = open(inputArray[redirectInPos + 1], O_RDONLY);
            } else {
                printColor("Error: Invalid input file path", "red", true);
                return -1;
            }//throw error because invalid input file
        }

        int fd_out = -1;
        if (redirectOutPos != -1) {
            fd_out = open(inputArray[redirectOutPos + 1], O_WRONLY | O_CREAT | O_TRUNC);//overwrite file if exists, create else

            if (inputArray.length <= redirectOutPos + 1) {
                printColor("Error: No output file specified after redirect output symbol", "red", true);
                return -1;
            }
        }

        String[] command = Arrays.copyOfRange(inputArray, 0, firstPos);
        int returnValue = parsePipe(command, fd_in, fd_out);


        
        return returnValue;

    }

    static int parsePipe(String[] inputArray, int fd_in, int fd_out) {

        String[][] command = splitArray(inputArray, "|");


        //execute commands
        int returnValue = 0;
        //first command gets stdin
        int lastIn = -1;
        int[] pipe1fd = new int[2];//array to pass to pipe as out parameter, then contains the read end[0] and write end[1]
        int[] pipe2fd = new int[2];
        int[] pipe3fd;
        if (command.length > 1) {
            returnValue = pipe(pipe1fd);
            if (returnValue != 0) {
                printColor("Error opening pipe", "red", true);
                return returnValue;
            }

            returnValue = execute(command[0], fd_in, pipe1fd[1]);//execute first command with potential input from file

            for (int i = 1; i < command.length - 1; i++) {
                returnValue = pipe(pipe2fd);
                if (returnValue != 0) {
                    printColor("Error opening pipe", "red", true);
                    return returnValue;
                }

                returnValue = greaterAbsolute(execute(command[i], pipe1fd[0], pipe2fd[1]), returnValue);

                //variable swap
                pipe3fd = pipe2fd;
                pipe2fd = pipe1fd;
                pipe1fd = pipe3fd;

            }
            lastIn = pipe1fd[0];
        } else {
            lastIn = fd_in;
        }
        // execute last (or only) command

        returnValue = greaterAbsolute(execute(command[command.length - 1], lastIn, fd_out), returnValue);


        return returnValue;
    }


    static int execute(String[] inputArray, int fd_in, int fd_out) {//0 for read, 1 for write
        //System.out.println(Arrays.toString(inputArray) + " fd_in: " + fd_in + " fd_out: " + fd_out);
        int[] intArray = new int[]{Integer.MIN_VALUE};//to pass to the waitpid function
        //split the Array into path and arguments

        //tries to find the executable at first position of input
        String path = null;
        if (inputArray.length > 0) {

            String input = inputArray[0];
           
            path = which(input);//"/bin/"+input;
            //}
        } else {
            printColor("Entered empty command", "red", true);
            return -1;
        }// internal error code


        //if path found, compile argslist and start execv
        if (isNumeric(path) == false) {
            printColor("Executing program at " + path, "green", true);

            int forkInt = fork();// saves the return value because every call will fork the process again


            //baby process
            if (forkInt == 0) {
                //Array[0] mit rest als parameter ausführen (path, args[])

                //read from pipe or write to it
                if (fd_in != -1) {
                    close(0);
                    dup2(fd_in, 0);
                }
                if (fd_out != -1) {
                    close(1);
                    dup2(fd_out, 1);
                }

                //execute the program
                if (execv(path, inputArray) < 0) {
                    printColor("execv fatal error", "red", true);
                    exit(1);
                }
                exit(0);


            }

            //papa process
            else {
                if (forkInt < 0) {
                    printColor("Error: fork() returned Error", "red", true);
                    return forkInt;
                }
                //close open files that only the child process needs
                if (fd_in != -1) {
                    close(fd_in);
                }
                if (fd_out != -1) {
                    close(fd_out);

                }

                //wait for child
                int f;
                if ((f = waitpid(forkInt, intArray, 0)) < 0) {
                    printColor("Error " + f + ": waiting for child", "red", true);
                    exit(1);
                }
                if (intArray[0] == 0) {
                    printColor("Process exited without error", "green", true);
                } else {
                    printColor("Process exited with error code " + intArray[0], "red", true);
                }


            }
        } else {
            printColor("Couldn't find executable assigned to \"" + inputArray[0] + "\"", "red", true);
        }
        return intArray[0];
    }

    public static ArrayList<Integer> findArrayOccurrence(Object[] oArr, Object o) {
        ArrayList<Integer> aList = new ArrayList<Integer>();
        for (int i = 0; i < oArr.length; i++) {
            if (oArr[i].equals(o)) {
                aList.add(i);
            }
        }
        return aList;
    }

    public static String[][] splitArray(String[] oArr, String o) {
        ArrayList<Integer> aList = findArrayOccurrence(oArr, o);
        aList.add(oArr.length);//emulate occurence of split object behind last array element to make handling easier in the for loop
        String[][] sArr = new String[aList.size()][];
        int prevOcc = 0;
        for (int i = 0; i < aList.size(); i++) {
            sArr[i] = Arrays.copyOfRange(oArr, prevOcc, aList.get(i));
            prevOcc = aList.get(i) + 1;
        }
        return sArr;
    }

    public static int greaterAbsolute(int a, int b) {
        if (Math.abs(a) > Math.abs(b)) {
            return a;
        } else {
            return b;
        }
    }


    public static String which(String arguments) {
        ProcessBuilder prcBuilder = new ProcessBuilder();
        prcBuilder.command("bash", "-c", "which " + arguments);


        try {

            Process process = prcBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));


            String line = null;
            StringBuffer buffer = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
            String path = buffer.toString();
            if (path.contains("/")) {
                return path;
            } else {
                int exitCode = process.waitFor();
                return Integer.toString(exitCode);
                //System.out.println("\nExited with error code : " + exitCode);
            }
        } catch (IOException e) {
            printColor("err1", "red", true);
        } catch (InterruptedException e) {
            printColor("err2", "red", true);
        }

        return "t";
    }


    public static boolean checkls(String input) {

        
        String[] inputArray = input.split("/");
        String checkfile = inputArray[inputArray.length - 1];
        StringBuffer buffer1 = new StringBuffer();
        if (input.indexOf("/") == 0) {
            buffer1.append("/");
        }
        for (int i = 0; i < inputArray.length; i++) {
            buffer1.append(inputArray[i]);
            if (i < inputArray.length - 1) {
                buffer1.append("/");
            }
        }

        ProcessBuilder prcBuilder = new ProcessBuilder();
        prcBuilder.command("bash", "-c", "ls " + buffer1.toString());


        try {

            Process process = prcBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));


            String line = null;
            StringBuffer buffer2 = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                buffer2.append(line);
                buffer2.append(" ");
            }
            String path = buffer2.toString();
            //printColor(path,"yellow",true);//for test purposes
            if (path.contains(checkfile)) {
                return true;
            } else {
                int exitCode = process.waitFor();
                return false;
                //System.out.println("\nExited with error code : " + exitCode);
            }
        } catch (IOException e) {
            printColor("err1", "red", true);
        } catch (InterruptedException e) {
            printColor("err2", "red", true);
        }

        return false;
    }

    public static boolean isNumeric(String toCheck) {
        try {
            double d = Double.parseDouble(toCheck);
        } catch (NumberFormatException nsfw) {
            return false;
        } catch (NullPointerException npe) {
            return false;
        }
        return true;

    }


    static boolean printColor(String text, String colorvar, boolean lineBreak) {// maybe rainbow??
        //takes two Strings and one boolean that specify the text to print, the color and if there should be a linebreak (ie print or println)

        String ANSI_RESET = "\u001B[0m";
        String ANSI_BLACK = "\u001B[30m";
        String ANSI_RED = "\u001B[31m";
        String ANSI_YELLOW = "\u001B[33m";
        String ANSI_BLUE = "\u001B[34m";
        String ANSI_PURPLE = "\u001B[35m";
        String ANSI_CYAN = "\u001B[36m";
        String ANSI_WHITE = "\u001B[37m";
        String ANSI_GREEN = "\u001B[32m";

        boolean returnBool = true;
        String color = "";

        switch (colorvar.toLowerCase()) {
            case "white":
                color = ANSI_WHITE;
                break;
            case "black":
                color = ANSI_BLACK;
                break;
            case "red":
                color = ANSI_RED;
                break;
            case "yellow":
                color = ANSI_YELLOW;
                break;
            case "blue":
                color = ANSI_BLUE;
                break;
            case "purple":
                color = ANSI_PURPLE;
                break;
            case "cyan":
                color = ANSI_CYAN;
                break;
            case "green":
                color = ANSI_GREEN;
                break;

            default:
                returnBool = false;
        }


        System.out.print(color + text + ANSI_RESET);

        if (lineBreak) {
            System.out.println();
        }
        return returnBool;
    }

    static String[] replaceCat(String[] args) {//used to integrate custom cat into shell
        if (args[0].equals("jcat")) {
            String[] changed = new String[args.length + 1];
            changed[0] = "java";
            changed[1] = "cat.java";

            for (int i = 1; i < args.length; i++) {//ignore the "jcat" at first pos
                changed[i + 1] = args[i];
            }

            return changed;
        } else {
            return args;
        }


    }


}