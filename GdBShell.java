import static cTools.KernelWrapper.*;	//imports a Java Wrapper for C kernel functions, needs the sourced source_me
import java.util.ArrayList;
import java.util.Scanner;
import java.lang.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Arrays;
import java.nio.charset.StandardCharsets;

//TODO switch case for printcolor
//TODO clean up ugly array handling that accounts for most of the lines

//TODO implement auto source?

public class main{

	public static void main(String[] args){
	

	System.out.println("Welcome!");
	String shellPrefix = "EN@GdBShell:";//print actual user instead of EN??
	int runningInt = 0;
	Scanner scn = new Scanner(System.in);	
		while (true){
			
			parseInput(getInput(runningInt,shellPrefix, scn)); 
			runningInt++;
			}
	

	}

	static String[] getInput(int runningInt, String shellPrefix, Scanner scn){
		
		
		String directoryPath = System.getProperty("user.dir");
		String[] directoryArray = directoryPath.split("/");
		StringBuffer buffer = new StringBuffer("~");
		

		//gets rid of the home/user directory and builds a string 
		for (int i=3;(i<directoryArray.length);i++){
			buffer.append("/");
			buffer.append(directoryArray[i]);
		}
		System.out.print("("+runningInt+")");
		printColor(shellPrefix,"purple",false);
		printColor(buffer.toString()+"$ ","cyan",false);
		
		String input= scn.nextLine();//get input

		String[] inputArray = input.split(" ");
		
		//get rid of spaces and tabs
		int countInputs = 0;
		int[] countArray = new int[inputArray.length];
		//count all inputs that actually contain something and put them in a new array in the second for loop
		for (int i =0;i< inputArray.length;i++){
			if (inputArray[i].equals(" ")||inputArray[i].equals("	")||inputArray[i].equals("")){
				;
			}
			else{
				countArray[countInputs]=i;
				countInputs++;
			}
		}
		//System.out.println(Arrays.toString(countArray));
		String[] returnArray = new String[countInputs];
		for (int i=0; i<countInputs;i++){
			returnArray[i]= inputArray[(countArray[i])];
			}

		return returnArray;
		//return replaceCat(returnArray);
	}

	static void parseInput(String[] inputArray){

		//System.out.println(inputArray.length + Arrays.toString(inputArray));
		//check for empty inputs
		if (inputArray.length<=1){
			if(inputArray.length==0){return;}
			if(inputArray[0].equals(" ")){return;}
			if(inputArray[0].equals("	")){return;}

		}

		//implements the exit terminal function
		if (inputArray[0].equals("exit")){
			if (inputArray.length==1){
				printColor("Session ended by user","red",true);
				System.exit(0);
			}
			else{
				printColor( "\"exit\" doesn't take parameters. It will end this current shell. Did you try to find a different executable?","red",true);
				return;
			}
			
		}



		//find concats with && in input and execute only if previous command was successful
		ArrayList<Integer> concatPosition = new ArrayList<Integer>();
		//save the positions of concats
		for (int i=0; i<inputArray.length;i++){
			if(inputArray[i].equals("&&")){
				concatPosition.add(i);
			}
		}

		/*
		//for testing***************, prints the positions of the concats
		for (int i=0;i<concatPosition.size();i++){
			printColor(Integer.toString(concatPosition.get(i))+" ","yellow",false);
		}

		//**************************
		*/

		//if there are any concats
		if(concatPosition.size()>0){
			
			//split the inputArray into singular commands that were separated by &&
				
			ArrayList<ArrayList<String>> commandsList = new ArrayList<ArrayList<String>>();
			for (int i=0;i<concatPosition.size()+1;i++){
				commandsList.add( new ArrayList<String>());
			}
			//upper Arraylist used with fixed size, doesnt throw warning like using an array would
			//because one concat splits the input into two parts --->+1
			
			//iterate over every command except last, because no concat comes after the last one
			int prevConcatPos=0;
			for (int i=0;i<concatPosition.size();i++){
				
				for(int j=prevConcatPos;j<concatPosition.get(i);j++){
					(commandsList.get(i)).add(inputArray[j]);
				}
				prevConcatPos=concatPosition.get(i)+1;
			}
			//add last command
			for (int i=concatPosition.get(concatPosition.size()-1)+1;i<inputArray.length;i++){
				(commandsList.get(commandsList.size()-1)).add(inputArray[i]);
			}

			//execute commands in commandsList if previous command was successful, ie terminated with code 0
			int returnValue = 0;
			for(int i=0;i<commandsList.size();i++){
				//if previous command successful
				if (returnValue==0){
					
					//build array from arraylist that contains command
					String[] command = new String[commandsList.get(i).size()];
					for (int j=0;j<command.length;j++){
						command[j]= commandsList.get(i).get(j);
					}

					//execute command and save return value
					returnValue = parseRedirect(command);
				}

				//if it wasnt successful
				else{
					//build command as string that wasnt succcessful
					int failedCommandNumber = i-1;
					StringBuffer buffer = new StringBuffer();
					for (int j=0;j<commandsList.get(failedCommandNumber).size();j++){
						buffer.append(commandsList.get(failedCommandNumber).get(j));
					}
					
					
					printColor("Stopped concatenation at command number "+ failedCommandNumber +" \""+buffer.toString()+"\"","red",true);

					if(returnValue==Integer.MIN_VALUE){
						printColor("Couldn't find executable assigned to \""+ commandsList.get(failedCommandNumber).get(0)+"\"","red",true);
					}
					else{
						printColor("Exited with error code "+returnValue,"red",true);
					}
					break;
				}



			}
		}


		// if its only one command without concats
		else{
			int returnValue = parseRedirect(inputArray);
			if (returnValue==0){
				printColor("Process exited without error","green",true);
			}
			else{
				if(returnValue == Integer.MIN_VALUE){
					printColor("Couldn't find executable assigned to \""+ inputArray[0]+"\"","red",true);
				}
				else{
					printColor("Process exited with code "+ returnValue,"red",true);
				}
			}
		}
		return;//TODO: clean up error messages
	}

	static int parseRedirect(String[] inputArray){//should take pipe and read/write,, add >> for append?
		//find last occurrences of < and >
		int redirectInPos = -1;
		int redirectOutPos = -1;
		boolean setFirstBool = true;
		int firstPos = inputArray.length;
		for(int i=0;i< inputArray.length;i++){
			if(inputArray[i].equals("<")){
				redirectInPos = i;
				if(setFirstBool){
					firstPos=i;
					setFirstBool = false;
				}
			}
			if(inputArray[i].equals(">")){
				redirectOutPos = i;
				if(setFirstBool){
					firstPos=i;
					setFirstBool = false;
				}
			}
			/*if(inputArray[i].equals(">>")){
				appendOutPos = i;
			}*/
		}

	
		
		int fd_in=-1;
		if(redirectInPos!=-1){
			if(checkls(inputArray[redirectInPos+1])){
				fd_in = open(inputArray[redirectInPos+1],O_RDONLY);
			}
			else{return -10;}//throw error because invalid input file
		}

		int fd_out=-1;
		if(redirectOutPos!=-1){
			fd_out = open(inputArray[redirectOutPos+1],O_WRONLY|O_CREAT|O_TRUNC);//create file if non existent, overwrite if it is
		}
		
		String[] commands = new String[firstPos];
		for (int i=0;i< commands.length;i++){
			commands[i]=inputArray[i];
		}
		int returnValue=  parsePipe(commands,fd_in,fd_out);
		if(fd_in!=-1){
			close(fd_in);
		}
		if(fd_out!= -1){
			close(fd_out);
		}
		return returnValue;

	}

	static int parsePipe(String[] inputArray,int fd_in, int fd_out){
		int returnValue = -5;
		ArrayList<Integer> pipePos = new ArrayList<Integer>();
		int count =0;
		for (int i =0;i< inputArray.length; i++){
			if (inputArray[i].equals("|")){
				pipePos.add(count);
				count=0;

			}
			else{count++;}
		}
		pipePos.add(count);
		
		//populate array of commands separated by |
		String[][] commands = new String[pipePos.size()][];
		int prevPipePos = 0;
		for (int i=0;i< pipePos.size();i++){
			commands[i] = new String[pipePos.get(i)];
			for (int j=0;j<commands[i].length;j++){
				commands[i][j]=inputArray[j+prevPipePos];
			}
			prevPipePos = prevPipePos + pipePos.get(i)+1;
		}
		
		//execute commands
		//first command gets stdin
		int lastIn = -1;
		int[] pipefd = new int[2];
		if(commands.length>1){
			pipe(pipefd);
			returnValue=  execute(commands[0],fd_in,pipefd[1]);
			for(int i=1;i<commands.length-1;i++){
				returnValue = execute(commands[i],pipefd[0],pipefd[1]);

			}
			lastIn = pipefd[0];
			System.err.println("here");
		}
		else{
			lastIn = fd_in;
		}
		// execute last (or only) command
		returnValue = execute(commands[commands.length-1],lastIn,fd_out);
		


		
		return returnValue;
	}

	

	
	
	static int execute(String[] inputArray,int fd_in, int fd_out){//0 for read, 1 for write
		int[] intArray = new int[]{Integer.MIN_VALUE};//to pass to the waitpid function
		//split the Array into path and arguments
		
		//tries to find the executable at first position of input
		String path = null;
		if (inputArray.length!=0){
			
			String input = inputArray[0];
			/*
			if (input.indexOf("/")>-1){
				if (checkls(input)==true){
					
					path = input;
				}
			}
			else{*/
				path = which(input);
			//}
		}
		else{return Integer.MIN_VALUE+1;}// add error in parseInput for this number

		
		
			
		//if path found, compile argslist and start execv	
		if (isNumeric(path)==false){
			printColor("Executing program at "+path,"green",true);
			//System.out.println();
			
			//int[] pipeArray = new int[]{0};//sets the pip
			int forkInt = fork();// saves the return value because every call will fork the process again
			

			

			//baby process
			if (forkInt==0){
				//Array[0] mit rest als parameter ausführen (path, args[])
				
				//read from pipe or write to it
				if(fd_in!=-1){
					close(0);
					dup2(fd_in,0);
				}
				if(fd_out!=-1){
					close(1);
					dup2(fd_out,1);
				}

				//execute the program
				execv(path, inputArray);
					printColor("execv fatal error","red",true);
					exit(0);
				
				
			}
			
			//papa process
			else{
			
			waitpid(forkInt,intArray,0);
			
			//read from pipe HEEERE???
			}
		}
		return intArray[0];
	}


	
	public static String which(String arguments){
		ProcessBuilder prcBuilder = new ProcessBuilder();
		prcBuilder.command("bash","-c","which "+arguments);
		
		
		
		
		try {

            Process process = prcBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            
            String line =null;
            StringBuffer buffer = new StringBuffer();
            while ((line=reader.readLine())!=null){
            	buffer.append(line);
            }
            String path = buffer.toString();
            if (path.contains("/")){
				return path;
			}
            

            else{
            	int exitCode = process.waitFor();
            	return Integer.toString(exitCode);
            	//System.out.println("\nExited with error code : " + exitCode);
            }
        } 
        catch (IOException e) {
			printColor("err1","red",true); //e.toString();
        } 
        catch (InterruptedException e) {
            printColor("err2","red",true); //e.toString();
        }

	return "t";
	}

	
	

	public static boolean checkls(String input){
		
		//check what ls does in this program if executed locally
		String[] inputArray = input.split("/");
		String checkfile = inputArray[inputArray.length-1];
		StringBuffer buffer1 = new StringBuffer();
		if (input.indexOf("/")==0){
			buffer1.append("/");
		}
		for (int i=0;i<inputArray.length;i++){
			buffer1.append(inputArray[i]);
			if(i<inputArray.length-1){
				buffer1.append("/");
			}
		}
		//System.out.println(buffer1);

		ProcessBuilder prcBuilder = new ProcessBuilder();
		prcBuilder.command("bash","-c","ls "+buffer1.toString());
		
		

		try {

            Process process = prcBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            
            String line =null;
            StringBuffer buffer2 = new StringBuffer();
            while ((line=reader.readLine())!=null){
            	buffer2.append(line);
            	buffer2.append(" ");
            }
            String path = buffer2.toString();
            //printColor(path,"yellow",true);//for test purposes
            if (path.contains(checkfile)){
				return true;
			}
            

            else{
            	int exitCode = process.waitFor();
            	return false;
            	//System.out.println("\nExited with error code : " + exitCode);
            }
        } 
        catch (IOException e) {
			printColor("err1","red",true); //e.toString();
        } 
        catch (InterruptedException e) {
            printColor("err2","red",true); //e.toString();
        }

	return false;
	}

	public static boolean isNumeric(String toCheck){
		try{
			double d = Double.parseDouble(toCheck);
		}
		catch(NumberFormatException nsfw){
			return false;
		}
		catch(NullPointerException npe){
			return false;
		}
		return true;

	}



	static boolean printColor(String text, String colorvar, boolean lineBreak){// maybe rainbow??
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

		boolean returnBool= true;
		String color= colorvar.toLowerCase();

		if (color.equals("white")){
			color=ANSI_WHITE;
		}
		else{
			if (color.equals("black")){
			color=ANSI_BLACK;
			}
			else{
				if (color.equals("yellow")){
				color=ANSI_YELLOW;
				}
				else{
					if (color.equals("blue")){
					color=ANSI_BLUE;
					}
					else{
						if (color.equals("purple")){
						color=ANSI_PURPLE;
						}
						else{
							if (color.equals("cyan")){
							color=ANSI_CYAN;
							}
							else{
								if (color.equals("red")){
								color=ANSI_RED;
								}
								else{
									if (color.equals("green")){
									color=ANSI_GREEN;
									}
									else{
										System.out.print(text);
										returnBool = false;
									}
								}
							}
						}
					}
				}
			}
		}

		System.out.print(color+text + ANSI_RESET);

		if (lineBreak){
			System.out.println("");
		}
		return returnBool;
	}

	static String[] replaceCat(String[] args){//used to integrate custom cat into shell
		if (args[0].equals("jcat")){
			String[] changed = new String[args.length+1];
			changed[0]= "java";
			changed[1]= "cat.java";

			for (int i=1;i<args.length;i++){//ignore the "jcat" at first pos
				changed[i+1]=args[i];
			}

			return changed;
		}
		else{return args;}

		
	}


	

}