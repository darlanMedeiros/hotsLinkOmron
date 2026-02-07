package demo;

import org.ctrl.DeviceImp;
import org.ctrl.DeviceRegisterImp;
import org.ctrl.IDevice;
import org.ctrl.IDeviceRegister;
import org.ctrl.comm.IComControl;
import org.ctrl.vend.omron.toolbus.ToolbusProtocol;
import org.ctrl.vend.omron.toolbus.commands.AreaReadDM;
import org.ctrl.vend.omron.toolbus.commands.AreaReadHR;
import org.ctrl.vend.omron.toolbus.commands.AreaWriteDM;
import org.ctrl.vend.omron.toolbus.commands.AreaWriteHR;
import org.ctrl.vend.omron.toolbus.commands.AreaWriteWR;
import org.ctrl.vend.omron.toolbus.memory.MemoryRead;
import org.ctrl.vend.omron.toolbus.memory.MemoryWrite;
import org.serial.SerialParameters;
import org.serial.SerialPort;
import org.serial.SerialPortFactoryPJC;
import org.serial.SerialPortHandlerPjcImp;
import org.serial.SerialUtils;

public class Test {

	// </editor-fold>//GEN-END:initComponents

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				initSystem();
			}
		});
	}

	public static void initSystem() {

		try {

			SerialParameters sp = new SerialParameters();

			sp.setDevice("COM2");
			// these parameters are set by default
			sp.setBaudRate(SerialPort.BaudRate.BAUD_RATE_9600);
			sp.setDataBits(7);
			sp.setParity(SerialPort.Parity.EVEN);
			sp.setStopBits(2);

			SerialUtils.setSerialPortFactory(new SerialPortFactoryPJC());

			plc = new DeviceImp(0, "CPM2A", "Test PLC", "PLC for communications test");

			deviceRegister = DeviceRegisterImp.getInstance();
			deviceRegister.addDevice(plc);

			comHandler = new SerialPortHandlerPjcImp(SerialUtils.createSerial(sp));

			if (comHandler instanceof IComControl) {
				((IComControl) comHandler).setCommunicationTimeOut(10000);
			}
			
			//Inicializa a tread
			comHandler.initialize();
			
			ToolbusProtocol toolbusProtocol = new ToolbusProtocol();
			
			AreaReadDM readDm = (AreaReadDM) toolbusProtocol.getCommand("RD"); 
			
			readDm.setTarget(plc);
			readDm.setLength(2);
			readDm.setAddress(1);			
		
			
			comHandler.setProtocolHandler(toolbusProtocol);
			
			
			
			comHandler.start();			
			

			if (comHandler.isStarted()) {
				// dataRead = new MemmoryRead(MemmoryRead.DM_WORD, 0, 10);
				// readData = new FinsProxy(plc, dataRead);
				
				//CRIA A MENSAGEM DM area ,
				//readData = new AreaReadDM(plc, 0, 1);			
//				
//				
//				//System.out.println("Test - READ DATA" + readData);
//				
//				//ENVIA A MENSAGEM PARA READ
//				
				comHandler.send(readDm);
//				
				System.out.println("Test - Mensagaem recebida >>>>>>>>>>>>>>>>>> "+ readDm.getReply());			
//						
//				
				//int[] dataBuff = readData.getReply().toHexArray();
				
			
//				
				//System.out.println("Test - RESPOSTA DO CLP "+ readData.getResponseStatusCode());		
//				
//				
//				//System.out.print("Test - Conteudo do dataBuff  >>>>>>>>>  " );
//
//				
				//int i = Integer.valueOf(readData.getReply().toString(), 16).intValue();
//				
				//System.out.println("Test - Valor convertido em inteiro   " + i );
//				
				//String str = Integer.toBinaryString(i);
//				
				
//				
				//System.out.println("Test - valor recebido em string >>>>>>" + readData.getReply().toString());
//				
//				String strBinario =  utils.getFormatedBinary(i);		
//				
//				
//				System.out.println("Test - Valor em binário convertido com OmronUtils " + strBinario);
//				
//				String recebido = readData.getReply().toString();
//				
//				System.out.println("Test - Valor em binário atraves de uma string     " + utils.getFormatedBinary(recebido));
//											
//				boolean[] bol = utils.getBolleanBits(strBinario);
//				
//				for (boolean b : bol) {
//					System.out.print("["+b+"]");
//				}
//				
//				System.out.println("\nTest - Tamanho da string    " + utils.getFormatedBinary(i).length() );
//				
//				System.out.println("Test - Valor em binário   " + str);
//				
//				System.out.println("Test - " + str.length());
//				
//			
//				
//				DataImp di = new DataImp(dataBuff);
//				
//				
//				
//				System.out.println("Test -  data inp is " + di.toString());
				//Read CIO area
				readHR = new AreaReadHR(plc,endereco , 1);				
				//comHandler.send(readHR);
				
			
				
				//int[] dataBuffCIO = readHR.getReply().toHexArray();				
				//DataImp diCIO = new DataImp(dataBuffCIO);				
				//System.out.println("Test - data inp is CIO " + diCIO.toString());	
				
				//System.out.println("Test - data inp is CIO " + diCIO.getLength());
				
				
				//Stop sistema
				
				//utils.getFormatedBinary(diCIO.toString());
				
				//System.out.println("Teste bits canal HR 0 >>>>>> " + utils.getFormatedBinary(diCIO.toString()));
			
				
			
				
				
			    int[] value = new int [2];
			    
			    value[0] = 10;
				value[1] = 11;
				
				writeHR = new AreaWriteHR(plc, endereco , value,MemoryWrite.BCD);
				
				writeWR = new AreaWriteWR(plc, endereco, value, MemoryWrite.BCD);
				
				//VERIFICAR O ERRO QUE ESTÁ OCORRENDO NO ENVIO DA MENSAGEM, ESTÁ ENVIANDO VALOR INCORRETO.
				
				//comHandler.send(writeHR);
				
				
				//comHandler.send(readHR);
				
				comHandler.send(writeWR);
				
				//System.out.println(writeHR.getResponseStatusCode());
				
				System.out.println(writeWR.getResponseStatusCode());
				
				//System.out.println("Valor enviado do HR " + endereco + " : "+ value[0]);
				
				//System.out.println("Valor rebido do HR" + endereco + " : "+ utils.convertHexDec(readHR.getReply().toString()));
				
				
				
				comHandler.stop();

				

			} else {
				System.out.println("comHandler no isStarted");
				comHandler.stop();
			}

		} catch (Exception e) {

			comHandler.stop();
			
			e.printStackTrace();

		}

	}

	

	// Variables declaration - do not modify//GEN-BEGIN:variables
	// End of variables declaration//GEN-END:variables
	// protected String comPort = "COM3";
	// protected int comSpeed = 57600;
	// protected boolean updateOnMin = true;
	protected static SerialPortHandlerPjcImp comHandler = null;
	protected static IDevice plc = null;
	protected static IDeviceRegister deviceRegister;
	
	protected static AreaReadDM readData;
	protected static AreaReadHR readHR;
	protected static AreaWriteHR writeHR;
	protected static AreaWriteDM writeDM;
	protected MemoryRead dataRead;
	protected static int  endereco = 0;
	protected static AreaWriteWR writeWR;
}
