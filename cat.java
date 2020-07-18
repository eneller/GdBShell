//the cat version that doesnt use lseek(), only read()


import static cTools.KernelWrapper.*;	//imports a Java Wrapper for C kernel functions, needs the sourced source_me
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Arrays;
import java.nio.charset.StandardCharsets;

//implement 
public class cat{

	public static void main(String[] args) {
		//double start = System.nanoTime();
		boolean[] errarr = {false};
		int count =0;
		boolean [] params = parseArgs(args);//extract parameters and remove them from args
		//System.out.println(Arrays.toString(args));//for testing
		boolean argsnull=true;
		for(int i=0;i<args.length;i++){
			if(args[i]!=null){
				argsnull=false;
				break;
			}
		}
		if (args.length==0||argsnull){//get input if without params
			inputLoop(params,count);
		}
		else{
			
			//System.out.println(Arrays.toString(args));//for testing
			if (params[0]){
				//System.out.println("If you always seek help from others, you will never be able to achieve greatness on your own.");// r/im14andthisisdeep
				printFile("cat.txt",params,errarr,count);
				
			}
			else{
				for (int i=0; i<args.length;i++){
					
					if (args[i]!=null){
						if (args[i].equals("-")){//get input if file name is "-"
							count = inputLoop(params,count);
						}
						else{
							count = printFile(args[i],params,errarr,count); //output file 
						}					
					}
				}
			}
			
		}
		/*
		double end = System.nanoTime()-start;
		end=end/(1000*1000*1000);			
		System.out.println(end);
		*/
		//System.out.println();
		if(errarr[0]){System.exit(1);}
		else{System.exit(0);}
	}

	static boolean[] parseArgs(String[] args){
		
		boolean[] params = new boolean[3];
		for (int i =0;i<args.length;i++){
			
			if(args[i].equals("--help")||args[i].equals("-s")||args[i].equals("-n")){
				
				if (args[i].equals("--help")){params[0]=true;} //display help
				if (args[i].equals("-s")){params[1]=true;} //suppress empty lines
				if (args[i].equals("-n")){params[2]=true;} //enumerate lines

				args[i] = null;
			}
		}
		
		return params;
	}

	static int inputLoop(boolean[] params, int count){
		Scanner scn = new Scanner(System.in);
		int countEmpty=0;
		boolean printThisLine = true;
		while (true){
			
			try{
				String buf = scn.nextLine();
				
				if(params[1]&&buf.length()==0){
					countEmpty++;
					if(countEmpty>1){
						printThisLine = false;
					}

				}
				else{
					countEmpty=0;
					printThisLine= true;
				}

				if(printThisLine){
					
					if(params[2]){
						System.out.print("\u001B[33m"+"["+Integer.toString(count)+"]"+"\u001B[0m");
						count++;
				}
				
				System.out.println(buf);
				}
			}
			catch(NoSuchElementException e){
				break;
			}
		}
		return count;
	}

	
	static int printFile(String f, boolean[] params, boolean[] errarr, int count){
		

		//System.out.println(Arrays.toString(params));
		int fd = open(f,O_RDONLY);
		int bufsz = Integer.MAX_VALUE/8; //test values?
		if (fd!=-1){
			byte[] bytes = new byte[bufsz];//add length
			int rd = read(fd,bytes,bufsz);
			close(fd);
	
			byte[] cleanbytes = new byte[rd];
			
			for (int i=0;i<rd;i++){
				cleanbytes[i]= bytes[i];
			}
			

			String s = new String(cleanbytes, StandardCharsets.UTF_8);
			String t = "";//length 0
			String u = "";
			
			

			if(params[1]){//suppress multiple empty lines
				
				for (int i=0;i<s.length();i++){
					
					if(params[1]==true&&s.charAt(i)=='\n'&&(t.length()>2&&t.charAt(t.length()-1)=='\n'&&t.charAt(t.length()-2)=='\n')){;}
					else{t = t+s.charAt(i);}
				}
			}
			else{t=s;}


			if(params[2]){//enumerate lines
				u = u + "\u001B[33m"+"["+Integer.toString(count)+"]"+"\u001B[0m";
				count++;

				for (int i=0;i<t.length();i++){
					u = u + t.charAt(i);
					if((t.charAt(i)=='\n')&&(i<(t.length()-1))){//if we just appended a line break and we arent at the end of the file
						u = u+"\u001B[33m"+"["+Integer.toString(count)+"]"+"\u001B[0m";
					//t=t+Integer.toString(count)+"    ";
					count++;
					}						
				}
			
			}
			else{u=t;}
			
			//possibly append newline at end of file?
			/*	
			if(u.charAt(u.length()-1)!='\n'){
				u = u+ '\n';
			}
			*/



			
			
			
			System.out.print(u);
		
				
			
			
			//System.out.println();
			return count;
		}
		else{
			errarr[0]=true;
			return count;
			}




		
	}


}