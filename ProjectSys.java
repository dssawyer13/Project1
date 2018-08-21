//Daniel Sawyer
//4/1/2018
//Systems Software
//Project 3
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Hashtable;
public class Proj4 {
	static String progName;
	static String[] words;
	static int[] locAdr = new int[10000];
	public static ArrayList<String> ogCode = new ArrayList<>();
	static int endLoc;
	static SymTab symTab = new SymTab();
	public static int registerB = 0;
	static String fName;
	
	public static void main(String[] args)
	{
		int[] locAdr = new int[10000]; 
				
		//***********Read in & trim file*********************//
		
		
		File comFile;
		if (0 < args.length) {
				fName = args[0];
	          comFile = new File(args[0]);
	      }
		else {
			return;
		}
	        BufferedReader br = null;
	        try {
	            String currentLine;
	            br = new BufferedReader(new FileReader(comFile));
	            while ((currentLine = br.readLine()) != null) {
	                if(currentLine.startsWith("."))
	                {
	                }else
	            	ogCode.add(currentLine.trim().replaceAll("\\s+", "\\$"));
	            }
	        } 
	        catch (IOException e) {
	        }   
	      //*********End Read in & Trim  file******************//       
	      //at this point the code has been trimmed and all whitespace has been replaced with \\$
	      //next step is to tokenize code
	        
	      //*********Process Code*********************/////
	        generatePass1(ogCode);
	       generatePass2();
	}//end main
	

	public static boolean generatePass1(ArrayList<String> ogCode)
	{
		String[] pass1Code = new String[10000];
		String pass1Line;
		int baseAddr = 0x0;
		String baseLabel = null;
		ArrayList<String> names = new ArrayList<String>();
		Optable.populate();
		int i = 0;
		int current=0x0;
		locAdr[0] = -1;
		Boolean findStart = false;
		int opLoc;
		opLoc = -1;
		while(!findStart) {
			words = ogCode.get(i).split("\\$");		
			if(ogCode.get(i).startsWith(".")) {
				i++;
			}
			else if((words[0].equals("START")))
			{
				progName="";
				locAdr[0]= Integer.parseInt(words[1],16);
				i++;
				findStart = true;
			}
			else if(words[1].equals("START"))
			{
				locAdr[0] = Integer.parseInt(words[2],16);
				progName = words[0];
				names.add(progName);
				symTab.putVal(progName, locAdr[0]);
				i++;
				findStart = true;
			}
			i++;
		}
		current = locAdr[0];
		
		for (int x = 1; x < ogCode.size(); x++)
		{
			locAdr[x] = current;
			words=ogCode.get(x).split("\\$");		
			if(ogCode.get(x).startsWith(".")){
			}
			else if(ogCode.get(x).startsWith("\\+"))
			{
				opLoc = 0;
			}
			else if(words.length == 3)
			{
				
				opLoc = 1;

			}
			else if(words.length == 2)
			{
				opLoc = 0;
			}
			else if(words.length == 1)
			{
				opLoc = 0;
			}
				else if(words[0] == "END")
			{
				opLoc = 0;
				endLoc = current;
				break;
			}
			else return false;
			String op = words[opLoc];
			
			if(Optable.getOpCode(op)!= -1 || ogCode.get(x).contains("RSUB"))
			{
				if(words.length ==3)
				{
					names.add(words[opLoc-1]);
					symTab.putVal(words[opLoc-1], current);
				}
				current=locAdr[x] + Optable.getMem(op);
			}
			else if(words[opLoc].equals("END"))
			{
				endLoc = current;
				break;
			}
			else if(words[opLoc].equals("WORD"))
			{
				if(symTab.getAddr(words[opLoc-1]) != -1)
				{
					System.out.println("Error at line " + (x+1) +" " + words[opLoc-1] + " already exists");
				}
				else
				{
					names.add(words[opLoc-1]);
					symTab.putVal(words[opLoc-1], current);
					current = locAdr[x] + 3;
				}
				
			}
			else if(words[opLoc].equals("RESW"))
			{
				if(symTab.getAddr(words[opLoc-1]) != -1)
				{
					System.out.println("Error at line " + (x+1) +" " + words[opLoc-1] + " already exists");
				}
				else
				{
					names.add(words[opLoc-1]);
					symTab.putVal(words[opLoc-1], current);
					current = locAdr[x] + (Integer.parseInt(words[2])*3);
				}
			}
			else if(words[opLoc].equals("RESB"))
			{
				if(symTab.getAddr(words[opLoc-1]) != -1)
				{
					System.out.println("error" + symTab.getAddr(words[opLoc-1]));
				}
				else
				{
					names.add(words[opLoc-1]);
					symTab.putVal(words[opLoc - 1], current);
					current = locAdr[x] + (Integer.parseInt(words[2],16));
				}
			}
			else if(words[opLoc].equals("BYTE"))
			{
				byte[] b;
				names.add(words[opLoc-1]);
				symTab.putVal(words[opLoc - 1], current);
				String[] temp = words[2].split("\'");

				if(words[2].contains("EOF")) current += 3;
				else try {
					b = temp[1].getBytes("UTF-8");
					current += b.length/2;
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}

			}
			else if(words[opLoc].equals("BASE"))
			{
				baseLabel = words[opLoc+1];
			}
			else
			{
				System.out.println("Invalid entry at line " + (x+1));
			}
		}
		return true;	
	}
	public static boolean generatePass2()
	{
		int q = 0;
		String[] textRecordOutput = new String[10000];
		int startAdr = locAdr[0];
		int x = 0;
		int opLoc, address = -1;
		ArrayList<String> textRecords = new ArrayList<String>(10000);
		words = ogCode.get(0).split("\\$");
		//make the header record
		String opc = null;
		int loaderA = 0x0;
		int registerX = 0x0;
		int recordSize = 0x0;
		int ff = 0;
		int programCounter = 0x0;
		//make the text records
		for(int i = 1; i< ogCode.size(); i++)
		{
			opc = "";
			programCounter = locAdr[i + 1];
			//set op location
			words = ogCode.get(i).split("\\$");
			for(int z = 0;z < words.length; z++)
			{
				System.out.print(words[z] + " ");
			}
			System.out.println();
			int plusLoc = -1;
			if(words[0].indexOf("+")!= -1)
				plusLoc = words[0].indexOf("+");
			else if (words[1].indexOf("+") != -1) {
				plusLoc = words[1].indexOf("+");
			}
		if(plusLoc != -1) {
				{
					int wordnum = 0;
					int opcode1 = Optable.getOpCode(words[0].replaceAll("\\+", ""));
					if(opcode1 == -1) {
						opcode1 = Optable.getOpCode(words[1].replaceAll("\\+", ""));
						wordnum = 1;
					}
					if(opcode1 != -1)
					{
						if(words[wordnum].contains("#"))//if immediate addressing
							{
							opcode1 += 1;
							opc += (String.format("%02x", (opcode1))) + "1";
							words[1].replaceAll("#", "");
							opc+=String.format("%05x", Integer.parseInt(words[1],16));
							}
						else {
								opcode1 += 3;
								opc += (String.format("%02x", (opcode1))) + "1";	
								opc += String.format("%05x", symTab.getAddr(words[1]));
						}
						textRecords.add(opc);
						textRecordOutput[i] = opc;
				}
			}
			}
			if(words.length == 3)
				{
					opLoc = 1;
				}
			else if (words.length == 2) {
				opLoc = 0;
				if(words[opLoc] == "LDB")
				{
					if(symTab.getAddr(words[opLoc + 1]) != -1) registerB += symTab.getAddr(words[opLoc + 1]);
					else
					{
					String temp = words[opLoc + 1];
						registerB = Integer.parseInt(temp.replaceAll("#", ""),16);
					}
				}
					
				if(words[opLoc] == "STA")
					{
						if(symTab.getAddr(words[opLoc + 1]) != -1) loaderA += symTab.getAddr(words[opLoc + 1]);
						else loaderA = Integer.parseInt(words[opLoc + 1], 16);
					}
				
				if(words[opLoc] == "LDX")
				{
					if(symTab.getAddr(words[opLoc + 1]) != -1) registerX += symTab.getAddr(words[opLoc + 1]);
					else registerX = Integer.parseInt(words[opLoc + 1], 16);
				}
				
			}//if length ==2
			
			else if (words[0].equals("END"))
				 {
					break;
				 }
			else return false;
			
			//temporary format 2 handlers
			if(words[opLoc + 1].contains("C'EOF'")) {
				opc = "454F46";
				textRecords.add(opc);
				textRecordOutput[i] = opc;
				
			}else
			if(words[0].contains("TIXR"))
			{
				opc = "B850";
				textRecords.add(opc);
				textRecordOutput[i] = opc;
				 
			}else
			if(words[0].contains("COMPR")) {
				opc = "A004"; 
				textRecords.add(opc);
				textRecordOutput[i] = opc;
				
			}	
			else
			if(words[opLoc] == "CLEAR") 
				{
					if(words[opLoc + 1] == "A")
						{
						loaderA = 0x0;
						opc = "B410";
						textRecords.add(opc);
						textRecordOutput[i] = opc;
 						}else
					if(words[opLoc + 1] == "B") 
					{
						registerB = 0x0;
						opc = "B400";
						textRecords.add(opc);
						textRecordOutput[i] = opc;
					}else
					if(words[opLoc + 1] == "X")
						{
						registerX = 0x0;
						opc = "B440";
						textRecords.add(opc);
						textRecordOutput[i] = opc;
						}
				}//end if clear
			//handle format 4 instructions before format 3
			
			//format 3	
			int opcode = Optable.getOpCode(words[opLoc]);
			if(opcode != -1) 
			{
				//immediate and direct mode
				if(words[opLoc + 1].startsWith("#"))
					{
					opcode += 3;
					opc = (String.format("%02x", opcode));
					opc += "0";
					opc += words[opLoc+1].replaceAll("#", "");
					} 
				//indirect addressing
				if(words[opLoc + 1].startsWith("@"))
				{
					opcode +=2;
					opc= (String.format("%02x", opcode));
					//try base relative
					if((((symTab.getAddr(words[opLoc+1].replaceAll("@", "")))- registerB) < 4095)&&(registerB > -1))
					{
						opc += "4";
						opc += (String.format("%03x", 
								(symTab.getAddr(words[opLoc+1].replaceAll("@", "")))- registerB ));
						textRecords.add(opc);
						textRecordOutput[i] = opc;
						
					}
					//try pc relative
					else if(((symTab.getAddr(words[opLoc+1].replaceAll("@","")) - programCounter) < 2047)
						&& ((symTab.getAddr(words[opLoc+1].replaceAll("@","")) - programCounter) > -2048))
					{
						opc += "2";
						opc += (String.format("%03x", (symTab.getAddr(words[opLoc+1].replaceAll("@","")) - programCounter)));
						textRecords.add(opc);
						textRecordOutput[i] = opc;
						
					}
					//try direct
					else
					{
						opc+= "0";
						opc += (String.format("%03x", symTab.getAddr(words[opLoc + 1].replaceAll("@",  ""))));			
						textRecords.add(opc);
						textRecordOutput[i] = opc;
						
					}
				}
				else 
					//if indexed addressing
					if(words[opLoc + 1].contains(",X"))
					{
						String symbol = words[opLoc + 1].split(",")[0];
						opc = (String.format("%02x", opcode + 3));
						//try base relative
						if((((symTab.getAddr(symbol))- registerB) < 4095)&&(registerB > -1))
						{
							opc += "C";
							opc += (String.format("%03x", 
									(symTab.getAddr(symbol))- registerB ));
							textRecords.add(opc);
							textRecordOutput[i] = opc;
							
						}
						//try pc relative
						else if(((symTab.getAddr(symbol) - programCounter) < 2047)
							&& ((symTab.getAddr(symbol) - programCounter) > -2048))
						{
							opc += "A";
							opc += (String.format("%03x", (symTab.getAddr(symbol) - programCounter)));
							textRecords.add(opc);
							textRecordOutput[i] = opc;
							
						}
						//try direct
						else
						{
							opc+= "8";
							opc += (String.format("%03x", symTab.getAddr(symbol)));			
							textRecords.add(opc);
							textRecordOutput[i] = opc;
							
						}
					}//end indexed mode
					else 
					{
						if((((symTab.getAddr(words[opLoc+1]))- registerB) < 4095)&&(registerB != 0))
						{
							opc = (String.format("%02x", opcode + 1));
							opc += "4";
							opc += (String.format("%03x", 
									(symTab.getAddr(words[opLoc+1]))- registerB));
							textRecords.add(opc);
							textRecordOutput[i] = opc;
							
						}
						//try pc relative
						else if(((symTab.getAddr(words[opLoc+1]) - programCounter) < 2047)
							&& ((symTab.getAddr(words[opLoc+1]) - programCounter) > -2048))
						{
							opc = (String.format("%02x", opcode + 3));
							opc += "2";
							opc += (String.format("%03x", (symTab.getAddr(words[opLoc+1]) - programCounter)));
							textRecords.add(opc);
							textRecordOutput[i] = opc;
							
						}
						//try direct
						else
						{
							opc = (String.format("%02x", opcode + 3));
							opc += (String.format("%04x", (symTab.getAddr(words[opLoc + 1].replaceAll("#",  "")))));			
							textRecords.add(opc);
							textRecordOutput[i] = opc;
							
						}					
					}
				
					if(opc != null) {
						x++;
					}
					for(int z = 0; z < textRecords.size(); z++)
					{
						System.out.print(textRecords.get(z).replaceAll("\\s+", "") + " ");
					}
					//Check if you need to go to the next line of the text record
					if(words[opLoc] == "RESW" )
					{
						opc = " ";
					}else
					if(words[opLoc] == "RESB" )
					{
						opc = " ";
						
					}else
					if(words[opLoc] == "WORD" )
					{
					opc = "00000";	
					textRecords.add(opc);
					textRecordOutput[i] = opc;
					
					}else
					if(words[opLoc] == "BYTE" )
					{
						opc = String.format("%02x", (words[opLoc + 1].replaceAll("'", "").replaceAll("X","")));
						textRecords.add(opc);
						textRecordOutput[i] = opc;
					}
					if(ogCode.get(i) != null) {
						//remember you can use the locAdr[1] for the initial starting address
						//Character to denote that there needs to be a text record break here.						
						q++;

					}
			
			}
		//make the end record
		}//for

		//add these records to the obj file
		final String PASS1FILE = fName.replaceAll(".txt", "") + ".lst";
		BufferedWriter bw = null;
		FileWriter fw = null;
		try {
			fw = new FileWriter(PASS1FILE);
			bw = new BufferedWriter(fw);
			
			for(int j = 0; j < ogCode.size(); j++)
			{
				if(textRecordOutput[j] == null)
				{
					textRecordOutput[j] = "\t\t";
				}
				String temp =String.format("%04x", (locAdr[j])) + "\t\t" + textRecordOutput[j] + "\t\t\t" + ogCode.get(j).replaceAll("\\$", "\t\t\t\t");
				System.out.println(temp);
				bw.newLine();
				bw.write(temp);
				bw.flush();
				endLoc = locAdr[j];
			}
			int tempNum = 0x0;
		}catch(IOException ex)
		{
			ex.printStackTrace();
		}finally {
			try {
				if(bw!=null)
					bw.close();
				if(fw!=null)
					fw.close();
			}catch(IOException ex1) {
				ex1.printStackTrace();
			}
		}
		final String PASS2FILE = fName.replaceAll(".txt", "") + ".obj";
		BufferedWriter bw1 = null;
		FileWriter fw1 = null;
		try {
			int z = 0;
			fw1 = new FileWriter(PASS2FILE);
			bw1 = new BufferedWriter(fw1);
			for(String g: textRecords)
			{
				bw1.newLine();
				bw1.write(g);
				bw1.flush();
				z++;
			}
		}catch(IOException ex)
		{
			ex.printStackTrace();
		}finally {
			try {
				if(bw1!=null)
					bw1.close();
				if(fw1!=null)
					fw1.close();
			}catch(IOException ex2) {
				ex2.printStackTrace();
			}
		}
		
		
				
		return true;
	}
	
	//Generate file to write to		
			
}//end project

class Optable{
static private Hashtable<String, Integer> opTab;
static private Hashtable<String, Integer> memTab;
static void populate()
{
	opTab=new Hashtable<String,Integer>();
	memTab =new Hashtable<String,Integer>();
	opTab.put("+LDB",0x68);
	opTab.put("MULR",0x98);
	opTab.put("+SSK",0xEC);
	opTab.put("WD",0xDC);
	opTab.put("*STX",0x10);
	opTab.put("*OR",0x44);
	opTab.put("AND",0x40);
	opTab.put("*LDA",0x00);
	opTab.put("+JGT",0x34);
	opTab.put("+STL",0x14);
	opTab.put("*WD",0xDC);
	opTab.put("+STI",0xD4);
	opTab.put("LPS",0xD0);
	opTab.put("+LDT",0x74);
	opTab.put("*LDCH",0x50);
	opTab.put("*LDL",0x08);
	opTab.put("TIXR",0xB8);
	opTab.put("SUBF",0x5C);
	opTab.put("*JSUB",0x48);
	opTab.put("LDX",0x04);
	opTab.put("+MULF",0x60);
	opTab.put("+J",0x3C);
	opTab.put("SVC",0xB0);
	opTab.put("STT",0x84);
	opTab.put("+COMP",0x28);
	opTab.put("TIX",0x2C);
	opTab.put("FLOAT",0xC0);
	opTab.put("LDT",0x74);
	opTab.put("STA",0x0C);
	opTab.put("*TD",0xE0);
	opTab.put("SHIFTR",0xA8);
	opTab.put("STB",0x78);
	opTab.put("SIO",0xF0);
	opTab.put("LDA",0x00);
	opTab.put("HIO",0xF4);
	opTab.put("+STS",0x7C);
	opTab.put("DIVF",0x64);
	opTab.put("*TIX",0x2C);
	opTab.put("+JSUB",0x48);
	opTab.put("LDCH",0x50);
	opTab.put("+COMPF",0x88);
	opTab.put("JEQ",0x30);
	opTab.put("*DIV",0x24);
	opTab.put("+STT",0x84);
	opTab.put("+SUBF",0x5C);
	opTab.put("*AND",0x40);
	opTab.put("+OR",0x44);
	opTab.put("SSK",0xEC);
	opTab.put("+JLT",0x38);
	opTab.put("*RD",0xD8);
	opTab.put("LDS",0x6C);
	opTab.put("*MUL",0x20);
	opTab.put("+LDS",0x6C);
	opTab.put("+DIV",0x24);  ;
	opTab.put("J",0x3C);
	opTab.put("+MUL",0x20);
	opTab.put("*COMP",0x28);
	opTab.put("+STX",0x10);
	opTab.put("*J",0x3C);
	opTab.put("+LDA",0x00);
	opTab.put("+SUB",0x1C);
	opTab.put("+STB",0x78);
	opTab.put("*JLT",0x38);
	opTab.put("SUB",0x1C);
	opTab.put("+ADDF",0x58);
	opTab.put("RD",0xD8);
	opTab.put("*JEQ",0x30);
	opTab.put("LDB",0x68);
	opTab.put("RSUB",0x4C);
	opTab.put("MULF",0x60);
	opTab.put("JSUB",0x48);
	opTab.put("SUBR",0x94);
	opTab.put("DIVR",0x9C);
	opTab.put("LDL",0x08);
	opTab.put("+JEQ",0x30);
	opTab.put("+STCH",0x54);
	opTab.put("*STL",0x14);
	opTab.put("+STA",0x0C);
	opTab.put("STSW",0xE8);
	opTab.put("COMPF",0x88);
	opTab.put("+DIVF",0x64);
	opTab.put("+STF",0x80);
	opTab.put("TIO",0xF8);
	opTab.put("*ADD",0x18);
	opTab.put("*STSW",0xE8);
	opTab.put("+STSW",0xE8);
	opTab.put("+LPS",0xD0);
	opTab.put("JLT",0x38);
	opTab.put("*JGT",0x34);
	opTab.put("MUL",0x20);
	opTab.put("+LDL",0x08);
	opTab.put("OR",0x44);
	opTab.put("COMP",0x28);
	opTab.put("TD",0xE0);
	opTab.put("STS",0x7C);
	opTab.put("*STCH",0x54);
	opTab.put("LDF",0x70);
	opTab.put("ADD",0x18);
	opTab.put("FIX",0xC4);
	opTab.put("*RSUB",0x4C);
	opTab.put("NORM",0xC8);
	opTab.put("STF",0x80);
	opTab.put("*LDX",0x04);
	opTab.put("CLEAR",0xB4);
	opTab.put("+RSUB",0x4C);
	opTab.put("ADDF",0x58);
	opTab.put("+WD",0xDC);
	opTab.put("+LDCH",0x50);
	opTab.put("+LDF",0x70);
	opTab.put("+LDX",0x04);
	opTab.put("STCH",0x54);
	opTab.put("+ADD",0x18);
	opTab.put("+AND",0x40);
	opTab.put("*SUB",0x1C);
	opTab.put("STX",0x10);
	opTab.put("RMO",0xAC);
	opTab.put("COMPR",0xA0);
	opTab.put("SHIFTL",0xA4);
	opTab.put("STL",0x14);
	opTab.put("+TD",0xE0);
	opTab.put("ADDR",0x90);
	opTab.put("STI",0xD4);
	opTab.put("+TIX",0x2C);
	opTab.put("*STA",0x0C);
	opTab.put("JGT",0x34);
	opTab.put("DIV",0x24);
	opTab.put("+RD",0xD8);

	memTab.put("+LDB",0x4);
	memTab.put("MULR",0x2);
	memTab.put("+SSK",0x4);
	memTab.put("WD",0x3);
	memTab.put("*STX",0x3);
	memTab.put("*OR",0x3);
	memTab.put("AND",0x3);
	memTab.put("*LDA",0x3);
	memTab.put("+JGT",0x4);
	memTab.put("+STL",0x4);
	memTab.put("*WD",0x3);
	memTab.put("+STI",0x4);
	memTab.put("LPS",0x3);
	memTab.put("+LDT",0x4);
	memTab.put("*LDCH",0x3);
	memTab.put("*LDL",0x3);
	memTab.put("TIXR",0x2);
	memTab.put("SUBF",0x3);
	memTab.put("*JSUB",0x3);
	memTab.put("LDX",0x3);
	memTab.put("+MULF",0x4);
	memTab.put("+J",0x3);
	memTab.put("SVC",0x2);
	memTab.put("STT",0x3);
	memTab.put("+COMP",0x4);
	memTab.put("TIX",0x3);
	memTab.put("FLOAT",0x1);
	memTab.put("LDT",0x3);
	memTab.put("STA",0x3);
	memTab.put("*TD",0x3);
	memTab.put("SHIFTR",0x2);
	memTab.put("STB",0x3);
	memTab.put("SIO",0x1);
	memTab.put("LDA",0x3);
	memTab.put("HIO",0x1);
	memTab.put("+STS",0x4);
	memTab.put("DIVF",0x3);
	memTab.put("*TIX",0x3);
	memTab.put("+JSUB",0x4);
	memTab.put("LDCH",0x3);
	memTab.put("+COMPF",0x4);
	memTab.put("JEQ",0x3);
	memTab.put("*DIV",0x3);
	memTab.put("+STT",0x4);
	memTab.put("+SUBF",0x4);
	memTab.put("*AND",0x3);
	memTab.put("+OR",0x4);
	memTab.put("SSK",0x3);
	memTab.put("+JLT",0x4);
	memTab.put("*RD",0x3);
	memTab.put("LDS",0x3);
	memTab.put("*MUL",0x3);
	memTab.put("+LDS",0x4);
	memTab.put("+DIV",0x4) ;  
	memTab.put("J",0x3);
	memTab.put("+MUL",0x4);
	memTab.put("*COMP",0x3);
	memTab.put("+STX",0x4);
	memTab.put("*J",0x3);
	memTab.put("+LDA",0x4);
	memTab.put("+SUB",0x4);
	memTab.put("+STB",0x4);
	memTab.put("*JLT",0x3);
	memTab.put("SUB",0x3);
	memTab.put("+ADDF",0x4);
	memTab.put("RD",0x3);
	memTab.put("*JEQ",0x3);
	memTab.put("LDB",0x3);
	memTab.put("RSUB",0x3);
	memTab.put("MULF",0x3);
	memTab.put("JSUB",0x3);
	memTab.put("SUBR",0x2);
	memTab.put("DIVR",0x2);
	memTab.put("LDL",0x3);
	memTab.put("+JEQ",0x4);
	memTab.put("+STCH",0x4);
	memTab.put("*STL",0x3);
	memTab.put("+STA",0x4);
	memTab.put("STSW",0x3);
	memTab.put("COMPF",0x3);
	memTab.put("+DIVF",0x4);
	memTab.put("+STF",0x4);
	memTab.put("TIO",0x1);
	memTab.put("*ADD",0x3);
	memTab.put("*STSW",0x3);
	memTab.put("+STSW",0x4);
	memTab.put("+LPS",0x4);
	memTab.put("JLT",0x3);
	memTab.put("*JGT",0x3);
	memTab.put("MUL",0x3);
	memTab.put("+LDL",0x4);
	memTab.put("OR",0x3);
	memTab.put("COMP",0x3);
	memTab.put("TD",0x3);
	memTab.put("STS",0x3);
	memTab.put("*STCH",0x3);
	memTab.put("LDF",0x3);
	memTab.put("ADD",0x3);
	memTab.put("FIX",0x1);
	memTab.put("*RSUB",0x3);
	memTab.put("NORM",0x1);
	memTab.put("STF",0x3);
	memTab.put("*LDX",0x3);
	memTab.put("CLEAR",0x2);
	memTab.put("+RSUB",0x4);
	memTab.put("ADDF",0x3);
	memTab.put("+WD",0x4);
	memTab.put("+LDCH",0x4);
	memTab.put("+LDF",0x4);
	memTab.put("+LDX",0x4);
	memTab.put("STCH",0x3);
	memTab.put("+ADD",0x4);
	memTab.put("+AND",0x4);
	memTab.put("*SUB",0x3);
	memTab.put("STX",0x3);
	memTab.put("RMO",0x2);
	memTab.put("COMPR",0x2);
	memTab.put("SHIFTL",0x2);
	memTab.put("STL",0x3);
	memTab.put("+TD",0x4);
	memTab.put("ADDR",0x2);
	memTab.put("STI",0x3);
	memTab.put("+TIX",0x4);
	memTab.put("*STA",0x3);
	memTab.put("JGT",0x3);
	memTab.put("DIV",0x3);
	memTab.put("+RD",0x4);
}

static public Integer getOpCode(String key)
{
	Integer code=opTab.get(key);
	if(code==null)	return -1;
	else return code;
}
static public Integer getMem(String key)
{
	Integer code=memTab.get(key);
	if(code==null)	return -1;
	else return code;
}
}

class SymTab{
	//The hash table
	private Hashtable<String,Integer> addr;
	//Constructor
	public SymTab()
	{
		addr=new Hashtable<String,Integer>();
	}
	//Interfacing methods
	public void putVal(String name,int address)
	{
		addr.put(name,address);
	}
	public int getAddr(String name)
	{
		Integer ad=addr.get(name);
		if(ad==null)	return -1;
		else return ad;
	}
}