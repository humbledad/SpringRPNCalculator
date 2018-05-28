package org.sanchez.userinterface;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.sanchez.utils.RPN_Calculator;

public class User {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		
        RPN_Calculator rpn_calculator = new RPN_Calculator();
        
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        
        String input;  

        System.out.println("Input: ");
        
        input = br.readLine();
        
        rpn_calculator.setClient_input(input);
        
        rpn_calculator.process_client_input();
        
        System.out.print(rpn_calculator.getRpn_stack());
        
        System.out.print(rpn_calculator.getError_message());
 		
		

	}

}
