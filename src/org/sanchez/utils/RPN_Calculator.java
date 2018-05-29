
package org.sanchez.utils;

import static java.lang.Long.parseLong;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sanchez.spring.MyRpnConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class RPN_Calculator {
    
    ArrayList<String> rpn_stack = null;  
    
    static enum ArithmeticType { ADDITION, SUBTRACTION, DIVISION, MULTIPLICATION };   
    
    static enum NumbersRequiredForCalculationType { ONE, TWO }; // sqrt rewuires 1 number.  ADDITION, SUBTRACTION, DIVISION, MULTIPLICATION require 2. 
    
    static enum WhichStackNumType { LAST, SECOND_LAST }; // right to left (stack functions from right to left) - last in first out - LIFO
    
    static enum WhichCommandLineNumType { FIRST, SECOND }; // left to right (command line functions from left to right) 
    
    private static final String UNWANTED_CHARACTERS = "[\\.$@~`#%^&()_=<>,:|,|;|']";
    
    private String client_input = null;
    
    private String stack_record = null;
    
    private String error_message = null;
  
    private ArrayList<String> log_calculations = new ArrayList<>(); //_delete_last_for_every_undo_call
    
    private ArrayList<String> stack_items_in_relation_to_current_operator = null;
   
    private ArrayList<String> command_line_items_left_of_current_operator = null;
    
    private String pre_stack_record = null;
    
    private  ApplicationContext spring_context = null; 
    
    private ApplicationContext spring_context_config = null;    
    
    public ApplicationContext getSpring_context_config() {
		return spring_context_config;
	}

	public void setSpring_context_config(ApplicationContext spring_context_config) {
		this.spring_context_config = spring_context_config;
	}

	private MyRpnConfig spring_config = new MyRpnConfig();
    
    public ApplicationContext getSpring_context() {
        return spring_context;
    }

    public void setSpring_context(ApplicationContext spring_context) {
        this.spring_context = spring_context;
    }    

    public String getStack_record() {
        return stack_record;
    }

    public void setStack_record(String stack_record) {
        this.stack_record = stack_record;
    }
    
    public String getError_message() {
        return error_message;
    }

    public void setError_message(String error_message) {
        this.error_message = error_message;
    }
    
    public ArrayList<String> getStack_items_in_relation_to_current_operator() {
        return stack_items_in_relation_to_current_operator;
    }

    public void setStack_items_in_relation_to_current_operator(ArrayList<String> stack_items_in_relation_to_current_operator) {
        this.stack_items_in_relation_to_current_operator = stack_items_in_relation_to_current_operator;
    }
    
    public ArrayList<String> getCommand_line_items_left_of_current_operator() {
        return command_line_items_left_of_current_operator;
    }

    public void setCommand_line_items_left_of_current_operator(ArrayList<String> command_line_items_left_of_current_operator) {
        this.command_line_items_left_of_current_operator = command_line_items_left_of_current_operator;
    }

    public String getClient_input() {
        return client_input;
    }

    public void setClient_input(String client_input) {
        this.client_input = client_input;
    }
    
private int check_for_unwanted_characters(){

    String raw = client_input;
    String cooked = raw.replaceAll(UNWANTED_CHARACTERS, spring_config.getEMPTY()); 
    if (client_input.contentEquals(cooked))
        return 0;
    else
        return -1;
}

private void replace_basic_operators_with_identifiers_that_the_apache_matcher_can_use(){

    if (client_input.indexOf('+') >= 0)
      client_input = client_input.replace( spring_config.getPLUS(), spring_config.getMATCHER_TOKEN_PLUS());

    if (client_input.indexOf('-') >= 0)
      client_input = client_input.replace( spring_config.getMINUS(), spring_config.getMATCHER_TOKEN_MINUS());

    if (client_input.indexOf('*') >= 0)
      client_input = client_input.replace( spring_config.getMULTIPLY(), spring_config.getMATCHER_TOKEN_MULTIPLY());

    if (client_input.indexOf('/') >= 0)
      client_input = client_input.replace( spring_config.getDIVIDE(), spring_config.getMATCHER_TOKEN_DIVIDE());

}

public String process_client_input(){

try {      
        this.error_message = spring_config.getEMPTY();

        if (check_for_unwanted_characters() == -1){
          this.error_message = spring_config.getERR_MSG_ILLEGAL_CHARACTERS_FOUND(); //"Error: Illegal characters were found in your Input. Only numbers and the following operators are allowed: sqrt clear undo + - * /";
          return this.error_message;
        }  

        replace_basic_operators_with_identifiers_that_the_apache_matcher_can_use();

        pre_stack_record = client_input;

        pre_stack_record = pre_stack_record.replace(spring_config.getDOUBLE_SPACE(), spring_config.getSINGLE_SPACE());

        List<String> tokens = new ArrayList<String>(); 

        tokens.add(spring_config.getMATCHER_TOKEN_SQUARE_ROOT()); tokens.add(spring_config.getMATCHER_TOKEN_CLEAR()); tokens.add(spring_config.getMATCHER_TOKEN_UNDO()); tokens.add(spring_config.getMATCHER_TOKEN_PLUS()); tokens.add(spring_config.getMATCHER_TOKEN_MINUS()); tokens.add(spring_config.getMATCHER_TOKEN_MULTIPLY()); tokens.add(spring_config.getMATCHER_TOKEN_DIVIDE());

        String patternString = "\\b(" + org.apache.commons.lang3.StringUtils.join(tokens, "|") + ")\\b";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(client_input);

        boolean is_operator_found = false;

        while (matcher.find()) {

            is_operator_found = true;

            String operator_name = matcher.group(1);
            int pos_first = matcher.start();
 

            if ( operator_name.contentEquals(spring_config.getMATCHER_TOKEN_DIVIDE()) ||  operator_name.contentEquals(spring_config.getMATCHER_TOKEN_MULTIPLY()) ||  operator_name.contentEquals(spring_config.getMATCHER_TOKEN_PLUS()) || operator_name.contentEquals(spring_config.getMATCHER_TOKEN_MINUS()) ) {

                process_std_arithmetic_operator (operator_name, pos_first);
            }

            else if (operator_name.contentEquals(spring_config.getMATCHER_TOKEN_SQUARE_ROOT())){  

                process_square_root_operator (operator_name, pos_first);
            }

            else if (operator_name.contentEquals(spring_config.getMATCHER_TOKEN_CLEAR())){   

                clear_stack();
            }

            else if (operator_name.contentEquals(spring_config.getMATCHER_TOKEN_UNDO())){   

                undo_previous_operation();
            }
        } 

        if (is_operator_found == false){

            process_non_occurence_of_legal_operators_only_apparently_numbers();
        } 
        
        
    }
    catch ( Exception ex){
        
        this.error_message = spring_config.getERR_MSG_IN_MAIN_PROCESS() + ex.toString();    
    }
    finally {}

    return stack_record;
    
 }

private void process_non_occurence_of_legal_operators_only_apparently_numbers(){
    rpn_stack.add(client_input);
    stack_record = client_input;
    log_calculations.add(client_input); // delete top item whenever a call to undo is made

}

private void clear_stack(){
    rpn_stack.clear();
    log_calculations.clear();
    stack_record = spring_config.getEMPTY();
}


private int undo_previous_operation(){

try {    
    
    String left_to_keep = spring_config.getEMPTY();
    
    String segment = rpn_stack.get(rpn_stack.size() - 1).toString();
    String[] array_values_between_spaces = segment.split(spring_config.getSINGLE_SPACE());  
    ArrayList<String> new_array_values_between_spaces = new ArrayList<>();
    for (int i = 0; i < array_values_between_spaces.length; i++){
    new_array_values_between_spaces.add(array_values_between_spaces[i]);
    if (i < (array_values_between_spaces.length - 1))
       left_to_keep = left_to_keep + spring_config.getSINGLE_SPACE() + array_values_between_spaces[i];
    }    
    String last_item = new_array_values_between_spaces.get(new_array_values_between_spaces.size()-1);
    int size_of_last_item = last_item.length();
    segment = segment.substring(0, segment.length() - size_of_last_item).trim();

    if (segment.contentEquals(spring_config.getEMPTY()))
    rpn_stack.remove(rpn_stack.size() - 1);
    else{
    rpn_stack.remove(rpn_stack.size() - 1);
    rpn_stack.add(segment);
    }

    final int size_of_undo = 4;

    int pos_after_first_undo_in_command_line = pre_stack_record.indexOf(spring_config.getMATCHER_TOKEN_UNDO()) + size_of_undo + 1;


    if ( pos_after_first_undo_in_command_line <= pre_stack_record.length() )
      pre_stack_record = pre_stack_record.substring(pos_after_first_undo_in_command_line, pre_stack_record.length());

    pre_stack_record = pre_stack_record.replace(spring_config.getDOUBLE_SPACE(), spring_config.getSINGLE_SPACE());   

    if (pre_stack_record.contains(spring_config.getMATCHER_TOKEN_CLEAR()))
     pre_stack_record = pre_stack_record.replace(spring_config.getMATCHER_TOKEN_CLEAR(), spring_config.getEMPTY());  

    if ( this.log_calculations.size() > 1 ) {
        
        this.rpn_stack.remove(rpn_stack.size() - 1);

        stack_record = left_to_keep + spring_config.getSINGLE_SPACE() + this.log_calculations.get(log_calculations.size() - 1);
        
        this.rpn_stack.add(stack_record);

        this.log_calculations.remove(log_calculations.size() - 1);        
    }
 
}
 catch ( Exception ex){
        
    this.error_message = spring_config.getERR_MSG_ERR_UNDOING_PREVIOS_OPERATION() + ex.toString();    

 }
 finally {}
return 0;
}


private int arithmetize_current_OPERATOR_on_2_STACK_numbers (String arithmetic_operator_name) {
try { 
    
    long arithmetic_result = 0;
    double division_result = 0;  

    String number_on_right = get_the_last_number_in_stack(WhichStackNumType.LAST, NumbersRequiredForCalculationType.TWO);
    long l_number_on_right = parseLong(number_on_right);

    String number_on_left = get_the_last_number_in_stack(WhichStackNumType.SECOND_LAST, NumbersRequiredForCalculationType.TWO);
    long l_number_on_left = parseLong(number_on_left);
   
    if (arithmetic_operator_name.contentEquals(spring_config.getMATCHER_TOKEN_MINUS()))
         arithmetic_result = l_number_on_left - l_number_on_right;
    else if (arithmetic_operator_name.contentEquals(spring_config.getMATCHER_TOKEN_PLUS()))
                arithmetic_result = l_number_on_left + l_number_on_right;
    else if (arithmetic_operator_name.contentEquals(spring_config.getMATCHER_TOKEN_MULTIPLY()))
                 arithmetic_result = l_number_on_left * l_number_on_right;
    else if (arithmetic_operator_name.contentEquals(spring_config.getMATCHER_TOKEN_DIVIDE()))
          division_result = (double)((double)l_number_on_left / (double)l_number_on_right);    

    String str_arithmetic_result = Long.toString(arithmetic_result);
    
    if (str_arithmetic_result.contains(spring_config.getDECIMAL_POINT_ZERO()))
      str_arithmetic_result = str_arithmetic_result.replace(spring_config.getDECIMAL_POINT_ZERO(), spring_config.getEMPTY());

    String str_division_result = Double.toString(division_result);
    
    if (str_division_result.contains(spring_config.getDECIMAL_POINT_ZERO()))
        str_division_result = str_division_result.replace(spring_config.getDECIMAL_POINT_ZERO(), spring_config.getEMPTY());    

    String id_to_find_position_of = number_on_left + spring_config.getSINGLE_SPACE() + number_on_right;
    
    String record_on_stack = this.rpn_stack.get(rpn_stack.size() - 1);
    
    String to_keep_right = record_on_stack.substring(record_on_stack.lastIndexOf(id_to_find_position_of) + id_to_find_position_of.length(), record_on_stack.length());

    int pos_left_number = record_on_stack.indexOf(id_to_find_position_of);// - 1; 
    String to_keep_left = record_on_stack.substring(0, pos_left_number);

    if (arithmetic_operator_name.contentEquals(spring_config.getMATCHER_TOKEN_DIVIDE()))
      stack_record = to_keep_left + spring_config.getSINGLE_SPACE() + str_division_result + spring_config.getSINGLE_SPACE() + to_keep_right; 
    else    
      stack_record = to_keep_left + spring_config.getSINGLE_SPACE() + str_arithmetic_result + spring_config.getSINGLE_SPACE() + to_keep_right;

    stack_record = stack_record.replace(spring_config.getDOUBLE_SPACE(), spring_config.getSINGLE_SPACE());    
    
    if (stack_record.contains(spring_config.getMATCHER_TOKEN_CLEAR()))
     stack_record = stack_record.replace(spring_config.getMATCHER_TOKEN_CLEAR(), spring_config.getEMPTY());   

    if (rpn_stack.size() > 0){ 
        rpn_stack.remove(rpn_stack.size() - 1);
        rpn_stack.add(stack_record);
    }
    else
        rpn_stack.add(stack_record);
        
    log_calculations.add(number_on_left + spring_config.getSINGLE_SPACE() + number_on_right); // delete top item whenever a call to undo is made
     
}
 catch ( NumberFormatException ex){
        
    this.error_message = spring_config.getERR_MSG_ERR_PROCESSING_OPERATOR_WITH_2_NOT_TOP_OF_STACK_NUMBERS() + ex.toString();    
 }
 finally {}

 return 0;
}



private int process_std_arithmetic_operator( String operator_name, int pos_first){
    

    int num_of_command_line_numbers = check_if_there_is_atleast_1_number_LEFT_of_current_operator_in_COMMAND_LINE (pos_first, NumbersRequiredForCalculationType.TWO);   
    

    if (num_of_command_line_numbers == 0){

        int num_of_stack_numbers = check_if_there_is_atleast_1_number_in_STACK (NumbersRequiredForCalculationType.TWO); 
        
        if (  (num_of_stack_numbers == 0) || (num_of_stack_numbers == 1)   ){ 
            // generate error message - cannot perform operation because 2 numbers are required} 
        }
        
        if (num_of_stack_numbers > 1){ 
            
            arithmetize_current_OPERATOR_on_2_STACK_numbers(operator_name); 
        }        
    }       

    else if (num_of_command_line_numbers == 1){

       arithmetize_current_OPERATOR_on_1_COMMAND_LINE_number_and_1_STACK_number(operator_name); 
         
    }

    else if (num_of_command_line_numbers == 2){
        
       arithmetize_current_OPERATOR_on_2_COMMAND_LINE_numbers(operator_name); 

    }
 
  return 0;

}


private int process_square_root_operator(String operator_name, int pos_first){



    int num_of_command_line_numbers = check_if_there_is_atleast_1_number_LEFT_of_current_operator_in_COMMAND_LINE (pos_first, NumbersRequiredForCalculationType.ONE);   

    if (num_of_command_line_numbers == 0){

        int num_of_stack_numbers = check_if_there_is_atleast_1_number_in_STACK (NumbersRequiredForCalculationType.ONE); 
        
        if (  (num_of_stack_numbers == 0) ){ 
            // generate error message - cannot perform operation because 1 number is required} 
        }
        
        if (num_of_stack_numbers > 0){ 
            
            int ret_val = process_SQUARE_ROOT_OPERATOR_on_1_STACK_number(operator_name); 
        }        
    }       

    else if (num_of_command_line_numbers > 0){

       int ret_val = process_SQUARE_ROOT_OPERATOR_on_1_COMMAND_LINE_number(operator_name); 
         
    }
      
  return 0;

}

private int arithmetize_current_OPERATOR_on_2_COMMAND_LINE_numbers (String arithmetic_operator_name) {

try { 
    
    long arithmetic_result = 0;
    double division_result = 0;  

    String number_on_left = this.command_line_items_left_of_current_operator.get(command_line_items_left_of_current_operator.size() - 2);
    String number_on_right = this.command_line_items_left_of_current_operator.get(command_line_items_left_of_current_operator.size() - 1);

    long l_number_on_left = parseLong(number_on_left);
    long l_number_on_right = parseLong(number_on_right);

    if (arithmetic_operator_name.contentEquals(spring_config.getMATCHER_TOKEN_MINUS()))
         arithmetic_result = l_number_on_left - l_number_on_right;
    else if (arithmetic_operator_name.contentEquals(spring_config.getMATCHER_TOKEN_PLUS()))
                arithmetic_result = l_number_on_left + l_number_on_right;
    else if (arithmetic_operator_name.contentEquals(spring_config.getMATCHER_TOKEN_MULTIPLY()))
                 arithmetic_result = l_number_on_left * l_number_on_right;
    else if (arithmetic_operator_name.contentEquals(spring_config.getMATCHER_TOKEN_DIVIDE()))
          division_result = (double)((double)l_number_on_left / (double)l_number_on_right);    

    String str_arithmetic_result = Long.toString(arithmetic_result);
    
    if (str_arithmetic_result.contains(spring_config.getDECIMAL_POINT_ZERO()))
      str_arithmetic_result = str_arithmetic_result.replace(spring_config.getDECIMAL_POINT_ZERO(), spring_config.getEMPTY());

    String str_division_result = Double.toString(division_result);
    
    if (str_division_result.contains(spring_config.getDECIMAL_POINT_ZERO()))
        str_division_result = str_division_result.replace(spring_config.getDECIMAL_POINT_ZERO(), spring_config.getEMPTY());    

    String id_to_find_position_of = number_on_left + spring_config.getSINGLE_SPACE() + number_on_right + spring_config.getSINGLE_SPACE() + arithmetic_operator_name;

    String to_keep_right = pre_stack_record.substring(pre_stack_record.indexOf(id_to_find_position_of) + id_to_find_position_of.length(), pre_stack_record.length());

    int pos_left_number = pre_stack_record.indexOf(id_to_find_position_of);// - 1; 
    String to_keep_left = pre_stack_record.substring(0, pos_left_number);

    if (arithmetic_operator_name.contentEquals(spring_config.getMATCHER_TOKEN_DIVIDE()))
      stack_record = to_keep_left + spring_config.getSINGLE_SPACE() + str_division_result + spring_config.getSINGLE_SPACE() + to_keep_right; 
    else    

    stack_record = to_keep_left + spring_config.getSINGLE_SPACE() + str_arithmetic_result + spring_config.getSINGLE_SPACE() + to_keep_right;

    stack_record = stack_record.replace(spring_config.getDOUBLE_SPACE(), spring_config.getSINGLE_SPACE());   
    
    if (stack_record.contains(spring_config.getMATCHER_TOKEN_CLEAR()))
     stack_record = stack_record.replace(spring_config.getMATCHER_TOKEN_CLEAR(), spring_config.getEMPTY());   
    

    pre_stack_record = stack_record;    

    if (!rpn_stack.isEmpty()) rpn_stack.remove(rpn_stack.size() - 1);

    rpn_stack.add(stack_record);
    
    log_calculations.add(number_on_left + spring_config.getSINGLE_SPACE() + number_on_right); // delete top item whenever a call to undo is made
    
}
 catch ( NumberFormatException ex){
        
    this.error_message = spring_config.getERR_MSG_ERR_PROCESSING_OPERATOR_WITH_2_NOT_TOP_OF_STACK_NUMBERS() + ex.toString();    
  }
 finally {}

 return 0;

}

// sqrt only requires 1 number obviously unlike + - / * that require 2 numbers
private int process_SQUARE_ROOT_OPERATOR_on_1_STACK_number ( String operator_name ) {

try { 

    String number_on_right = get_the_last_number_in_stack(WhichStackNumType.LAST, NumbersRequiredForCalculationType.ONE);

    long l_number_on_right = parseLong(number_on_right);
 
    double square_rooted_result = (double) Math.sqrt( l_number_on_right );
    
    String str_square_rooted_num = Double.toString(square_rooted_result);
    
    if (str_square_rooted_num.contains(spring_config.getDECIMAL_POINT_ZERO()))
    
        str_square_rooted_num = str_square_rooted_num.replace(spring_config.getDECIMAL_POINT_ZERO(), spring_config.getEMPTY());            

    String id_to_find_position_of = null;
    
    id_to_find_position_of = number_on_right; // + SINGLE_SPACE + operator_name;
    
    String record_on_stack = this.rpn_stack.get(rpn_stack.size() - 1);
    
    String to_keep_right = record_on_stack.substring(record_on_stack.lastIndexOf(id_to_find_position_of) + id_to_find_position_of.length(), record_on_stack.length());

    int pos_left_number = record_on_stack.indexOf(id_to_find_position_of);// - 1; 
    String to_keep_left = record_on_stack.substring(0, pos_left_number);

    stack_record = to_keep_left + spring_config.getSINGLE_SPACE() + str_square_rooted_num + spring_config.getSINGLE_SPACE() + to_keep_right;

    stack_record = stack_record.replace(spring_config.getDOUBLE_SPACE(), spring_config.getSINGLE_SPACE());    
    
    if (stack_record.contains(spring_config.getMATCHER_TOKEN_CLEAR()))
     stack_record = stack_record.replace(spring_config.getMATCHER_TOKEN_CLEAR(), spring_config.getEMPTY());   


    pre_stack_record = spring_config.getEMPTY();

    if (!rpn_stack.isEmpty()) rpn_stack.remove(rpn_stack.size() - 1);

    rpn_stack.add(stack_record);
    
    log_calculations.add(number_on_right); // delete top item whenever a call to undo is made
    
}
 catch ( NumberFormatException ex){
        
    this.error_message = spring_config.getERR_MSG_ERR_PROCESSING_SQUARE_ROOT_NOT_TOP_OF_STACK() + ex.toString();  
    
 }
 finally {}

 return 0;

}

// sqrt only requires 1 number obviously unlike + - / * that require 2 numbers
private int process_SQUARE_ROOT_OPERATOR_on_1_COMMAND_LINE_number ( String operator_name ) {

try { 

    String number_on_right = this.command_line_items_left_of_current_operator.get(command_line_items_left_of_current_operator.size() - 1);

    long l_number_on_right = parseLong(number_on_right);

         
    double square_rooted_result = (double) Math.sqrt( l_number_on_right );
    
    String str_square_rooted_num = Double.toString(square_rooted_result);
    
    if (str_square_rooted_num.contains(spring_config.getDECIMAL_POINT_ZERO()))
    
        str_square_rooted_num = str_square_rooted_num.replace(spring_config.getDECIMAL_POINT_ZERO(), spring_config.getEMPTY());            

    String id_to_find_position_of = null;
    
    id_to_find_position_of = number_on_right + spring_config.getSINGLE_SPACE() + operator_name;

    String to_keep_right = pre_stack_record.substring(pre_stack_record.indexOf(id_to_find_position_of) + id_to_find_position_of.length(), pre_stack_record.length());
    
    int pos_number = pre_stack_record.lastIndexOf(id_to_find_position_of);
    
    String to_keep_left = pre_stack_record.substring(0, pos_number);

    stack_record = to_keep_left + spring_config.getSINGLE_SPACE() + str_square_rooted_num + spring_config.getSINGLE_SPACE() + to_keep_right;

    stack_record = stack_record.replace(spring_config.getDOUBLE_SPACE(), spring_config.getSINGLE_SPACE());   
    
    if (stack_record.contains(spring_config.getMATCHER_TOKEN_CLEAR()))
        
      stack_record = stack_record.replace(spring_config.getMATCHER_TOKEN_CLEAR(), spring_config.getEMPTY());   

    pre_stack_record = stack_record;    

    if (!rpn_stack.isEmpty()) rpn_stack.remove(rpn_stack.size() - 1);

    rpn_stack.add(stack_record);
    
    log_calculations.add(number_on_right); // delete top item whenever a call to undo is made

}
 catch ( NumberFormatException ex){
        
    this.error_message = spring_config.getERR_MSG_ERR_PROCESSING_SQUARE_ROOT_TOP_OF_STACK() + ex.toString();   
  }
 finally {}

 return 0;

}



private String get_the_last_number_in_stack( WhichStackNumType last_or_secondlast , NumbersRequiredForCalculationType one_or_two){

 String ret_val = null;   
   
 try {   

        int num_of_stack_numbers = check_if_there_is_atleast_1_number_in_STACK (one_or_two);

        if (num_of_stack_numbers > 0){

        String last_record_on_stack = rpn_stack.get(rpn_stack.size() - 1);

        String[] array_values_between_spaces = last_record_on_stack.split(spring_config.getSINGLE_SPACE());

        if (last_or_secondlast == WhichStackNumType.LAST)
            
            return array_values_between_spaces[array_values_between_spaces.length - 1];
        
        else if (last_or_secondlast == WhichStackNumType.SECOND_LAST)
            
            return array_values_between_spaces[array_values_between_spaces.length - 2];

    }
}
 catch ( Exception ex){
        
    this.error_message = spring_config.getERR_MSG_ERR_OBTAINING_NUMBER_FROM_NOT_TOP_OF_STACK() + ex.toString(); 
 }
 finally {}    
 
 
return ret_val;

}

private int arithmetize_current_OPERATOR_on_1_COMMAND_LINE_number_and_1_STACK_number (String arithmetic_operator_name) {
try { 
    
    long arithmetic_result = 0;
    double division_result = 0;  

    String number_on_right = this.command_line_items_left_of_current_operator.get(command_line_items_left_of_current_operator.size() - 1);
    long l_number_on_right = parseLong(number_on_right);

    String number_on_left = get_the_last_number_in_stack(WhichStackNumType.LAST, NumbersRequiredForCalculationType.TWO);
    long l_number_on_left = parseLong(number_on_left);
   
    if (arithmetic_operator_name.contentEquals(spring_config.getMATCHER_TOKEN_MINUS()))
         arithmetic_result = l_number_on_left - l_number_on_right;
    else if (arithmetic_operator_name.contentEquals(spring_config.getMATCHER_TOKEN_PLUS()))
                arithmetic_result = l_number_on_left + l_number_on_right;
    else if (arithmetic_operator_name.contentEquals(spring_config.getMATCHER_TOKEN_MULTIPLY()))
                 arithmetic_result = l_number_on_left * l_number_on_right;
    else if (arithmetic_operator_name.contentEquals(spring_config.getMATCHER_TOKEN_DIVIDE()))
          division_result = (double)((double)l_number_on_left / (double)l_number_on_right);    

    String str_arithmetic_result = Long.toString(arithmetic_result);
    
    if (str_arithmetic_result.contains(spring_config.getDECIMAL_POINT_ZERO()))
      str_arithmetic_result = str_arithmetic_result.replace(spring_config.getDECIMAL_POINT_ZERO(), spring_config.getEMPTY());

    String str_division_result = Double.toString(division_result);
    
    if (str_division_result.contains(spring_config.getDECIMAL_POINT_ZERO()))
        str_division_result = str_division_result.replace(spring_config.getDECIMAL_POINT_ZERO(), spring_config.getEMPTY());    

    String id_to_find_position_of = number_on_right + spring_config.getSINGLE_SPACE() + arithmetic_operator_name;

    String to_keep_right = pre_stack_record.substring(pre_stack_record.indexOf(id_to_find_position_of) + id_to_find_position_of.length(), pre_stack_record.length());

    int pos_left_number = pre_stack_record.indexOf(id_to_find_position_of);// - 1; 
    String to_keep_left = pre_stack_record.substring(0, pos_left_number);

    if (arithmetic_operator_name.contentEquals(spring_config.getMATCHER_TOKEN_DIVIDE()))
      stack_record = to_keep_left + spring_config.getSINGLE_SPACE() + str_division_result + spring_config.getSINGLE_SPACE() + to_keep_right; 
    else    
      stack_record = to_keep_left + spring_config.getSINGLE_SPACE() + str_arithmetic_result + spring_config.getSINGLE_SPACE() + to_keep_right;

    stack_record = stack_record.replace(spring_config.getDOUBLE_SPACE(), spring_config.getSINGLE_SPACE());  
    
    if (stack_record.contains(spring_config.getMATCHER_TOKEN_CLEAR()))
     stack_record = stack_record.replace(spring_config.getMATCHER_TOKEN_CLEAR(), spring_config.getEMPTY());   

    pre_stack_record = stack_record; 

    if (!rpn_stack.isEmpty()) rpn_stack.remove(rpn_stack.size() - 1);

    rpn_stack.add(stack_record);
    
    log_calculations.add(number_on_left + spring_config.getSINGLE_SPACE() + number_on_right); // delete top item whenever a call to undo is made
}
 catch ( NumberFormatException ex){
        
    this.error_message = spring_config.getERR_MSG_ERR_PROCESSING_1_NUMBER_FROM_TOP_OF_STACK_AND_OTHER_FROM_SECOND_FROM_TOP() + ex.toString();    
  }
 finally {}

 return 0;
}


private int check_if_there_is_atleast_1_number_LEFT_of_current_operator_in_COMMAND_LINE (int position_1st_char_CURRENT_OPERATOR, NumbersRequiredForCalculationType one_or_two) {
    
try { 
    if (position_1st_char_CURRENT_OPERATOR == 0) return 0;
    
    if ( (this.pre_stack_record.indexOf(spring_config.getMATCHER_TOKEN_MINUS()) == 0) || (this.pre_stack_record.indexOf(spring_config.getMATCHER_TOKEN_PLUS()) == 0) || (this.pre_stack_record.indexOf(spring_config.getMATCHER_TOKEN_MULTIPLY()) == 0) || (this.pre_stack_record.indexOf(spring_config.getMATCHER_TOKEN_DIVIDE()) == 0))
            return 0;
    
    String last_item_val = null;
    
    String second_last_item_val = null;
    
    String segment_left_of_operator = client_input.substring(0, position_1st_char_CURRENT_OPERATOR - 1);    

    String[] array_values_between_spaces = segment_left_of_operator.split(spring_config.getSINGLE_SPACE());

    this.command_line_items_left_of_current_operator = new ArrayList<>();

    for (int i = 0; i < array_values_between_spaces.length; i++){

        this.command_line_items_left_of_current_operator.add(array_values_between_spaces[i]); 
    }
    
    if (command_line_items_left_of_current_operator.size() > 1){
        
        int num_numeric_items = 0;
        
        last_item_val = this.command_line_items_left_of_current_operator.get(command_line_items_left_of_current_operator.size() - 1);
        
        if (isNumeric(last_item_val)) num_numeric_items++;
    
        second_last_item_val = this.command_line_items_left_of_current_operator.get(command_line_items_left_of_current_operator.size() - 2);
        
        if (isNumeric(second_last_item_val)) num_numeric_items++;
        
        if (num_numeric_items == 2) 
            return 2;
        else { //generate error because you need 2 consecutive numbers to process, cannot go to stack because there is a non_numeric_in_between 
            
            if (one_or_two == NumbersRequiredForCalculationType.TWO){
                
              generate_last_result_with_error_message(spring_config.getERR_MSG_INSUFFIENT_NUMBERS_TOO_MANY_OPERATORS());
              
              return -2;
            }
            else {
                
                if ( (one_or_two == NumbersRequiredForCalculationType.ONE) && ( isNumeric(last_item_val) ) )
                    return 1;   
                else {
                    
                    generate_last_result_with_error_message(spring_config.getERR_MSG_SQRT_REQUIRED_A_NUMBER_TO_PRECEDE_IT());
                    
                    return -3;
                }
            }
            
        }  
    }
    
    else if (command_line_items_left_of_current_operator.size() == 1){
        
        last_item_val = this.command_line_items_left_of_current_operator.get(command_line_items_left_of_current_operator.size() - 1);
        
        if (isNumeric(last_item_val)) 
            return 1;
        else {  //generate error because you need 2 consecutive numbers to process, cannot go to stack because there is a non_numeric_in_between 
         
            generate_last_result_with_error_message(spring_config.getERR_MSG_NUMERIC_VALUE_EXPECTED());
            
            return -1;
        }    
    }
    
}
 catch ( NumberFormatException ex){
        
    this.error_message = spring_config.getERR_MSG_ERR_CHECK_IF_VALID_NUMBER_EXISTS_TOP_OF_STACK() + ex.toString();  
  }
 finally {}
 
  return 0;

}

private int generate_last_result_with_error_message(String message){

    List<String> tokens = new ArrayList<String>();
    tokens.add(spring_config.getMATCHER_TOKEN_SQUARE_ROOT());
    tokens.add(spring_config.getMATCHER_TOKEN_CLEAR());
    tokens.add(spring_config.getMATCHER_TOKEN_UNDO());
    tokens.add(spring_config.getMATCHER_TOKEN_PLUS());
    tokens.add(spring_config.getMATCHER_TOKEN_MINUS());
    tokens.add(spring_config.getMATCHER_TOKEN_MULTIPLY());
    tokens.add(spring_config.getMATCHER_TOKEN_DIVIDE());

    String patternString = "\\b(" + org.apache.commons.lang3.StringUtils.join(tokens, "|") + ")\\b";
    Pattern pattern = Pattern.compile(patternString);
    Matcher matcher = pattern.matcher(stack_record);
    
    while (matcher.find()) {

        
        String operator_name = matcher.group(1);
        int pos_first = matcher.start();
        int pos_last = matcher.end() - 1;

       if ( operator_name.contentEquals(spring_config.getMATCHER_TOKEN_DIVIDE()) ||  operator_name.contentEquals(spring_config.getMATCHER_TOKEN_MULTIPLY()) ||  operator_name.contentEquals(spring_config.getMATCHER_TOKEN_PLUS()) || operator_name.contentEquals(spring_config.getMATCHER_TOKEN_MINUS()) ) {    
        
          int pos_first_operator_found = stack_record.indexOf(operator_name);
          
          String segment = stack_record.substring(0, pos_first_operator_found);
          
          String[] array_values_between_spaces = segment.split(spring_config.getSINGLE_SPACE());
          
          String left_number = array_values_between_spaces[array_values_between_spaces.length - 2];
          
          String right_number = array_values_between_spaces[array_values_between_spaces.length - 1];
          
          long l_left_numer = Long.parseLong(left_number);
          
          long l_right_number = Long.parseLong(right_number);
          
          long last_calculated = 0;
          
          if (operator_name.contentEquals(spring_config.getMATCHER_TOKEN_DIVIDE())){ 
              last_calculated = l_left_numer / l_right_number;
              error_message = spring_config.getERR_MSG_ERR_LAST_OP_WAS() + left_number + spring_config.getDOUBLE_SPACE() + right_number + spring_config.getDIVIDE_AND_FULLSTOP()  + message  + left_number + spring_config.getAND() + right_number + spring_config.getERR_MSG_WERE_NOT_PUSHED_ONTO_STACK();
          }    
          if (operator_name.contentEquals(spring_config.getMATCHER_TOKEN_MULTIPLY())){ 
              last_calculated = l_left_numer * l_right_number;
              error_message = spring_config.getERR_MSG_ERR_LAST_OP_WAS() + left_number + spring_config.getDOUBLE_SPACE() + right_number + spring_config.getMULTIPLY_AND_FULLSTOP() + message  + left_number + spring_config.getAND() + right_number + spring_config.getERR_MSG_WERE_NOT_PUSHED_ONTO_STACK();
          }    
          if (operator_name.contentEquals(spring_config.getMATCHER_TOKEN_PLUS())){ 
              last_calculated = l_left_numer + l_right_number;
              error_message = spring_config.getERR_MSG_ERR_LAST_OP_WAS() + left_number + spring_config.getDOUBLE_SPACE() + right_number + spring_config.getPLUS_AND_FULLSTOP() + message  + left_number + spring_config.getAND() + right_number + spring_config.getERR_MSG_WERE_NOT_PUSHED_ONTO_STACK();
          }    
          if (operator_name.contentEquals(spring_config.getMATCHER_TOKEN_MINUS())) {
              last_calculated = l_left_numer - l_right_number;
              error_message = spring_config.getERR_MSG_ERR_LAST_OP_WAS() + left_number + spring_config.getDOUBLE_SPACE() + right_number + spring_config.getSUBTRACT_AND_FULLSTOP() +  message  + left_number + spring_config.getAND() + right_number + spring_config.getERR_MSG_WERE_NOT_PUSHED_ONTO_STACK();
          }    
          
          stack_record = spring_config.getEMPTY() + last_calculated;        
          
          break;
      } 
      else if (operator_name.contentEquals(spring_config.getMATCHER_TOKEN_SQUARE_ROOT())){
      
          int pos_first_operator_found = stack_record.indexOf(operator_name);
          
          String segment = stack_record.substring(0, pos_first_operator_found);
          
          String[] array_values_between_spaces = segment.split(spring_config.getSINGLE_SPACE());
                    
          String right_number = array_values_between_spaces[array_values_between_spaces.length - 1];
          
          long l_right_number = Long.parseLong(right_number);
         
          double square_rooted_result = (double) Math.sqrt( l_right_number );
       
          stack_record = spring_config.getEMPTY() + square_rooted_result;
          
          error_message = spring_config.getERR_MSG_ERR_LAST_OP_PERFORMED_WAS_SQRT() + right_number + ". " + message + spring_config.getSINGLE_SPACE() + right_number + spring_config.getERR_MSG_WAS_NOT_PUSHED_ONTO_STACK();

          break;
      
      }      
    }
    return 0;
}



public static boolean isNumeric(String str)
{
  return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
}

private int check_if_there_is_atleast_1_number_in_STACK (NumbersRequiredForCalculationType one_or_two) {
    
try {
    
    String last_item_val = null;
    
    String second_last_item_val = null;
    
    String stack_segment = null;
    
    if (rpn_stack.size() == 0)
        return 0;
    else
       stack_segment = rpn_stack.get(rpn_stack.size() - 1);

    String[] array_values_between_spaces = stack_segment.split(spring_config.getSINGLE_SPACE());

    this.stack_items_in_relation_to_current_operator = new ArrayList<>();

    for (int i = 0; i < array_values_between_spaces.length; i++){

        this.stack_items_in_relation_to_current_operator.add(array_values_between_spaces[i]); 
    }
    
    if (stack_items_in_relation_to_current_operator.size() > 1){
        
        last_item_val = this.stack_items_in_relation_to_current_operator.get(stack_items_in_relation_to_current_operator.size() - 1);
        
        boolean is_last_item_numeric = isNumeric(last_item_val);
    
        second_last_item_val = this.stack_items_in_relation_to_current_operator.get(stack_items_in_relation_to_current_operator.size() - 2);
        
        boolean is_second_last_item_numeric = isNumeric(second_last_item_val);
        
      if (is_last_item_numeric == false){ 
       
          generate_last_result_with_error_message(spring_config.getERR_MSG_LAST_ITEM_HAS_TO_BE_NUMERIC());
          
          return -2; // generate error because last item has to be numeric to be usable
      }
          
     
      else if ( (is_last_item_numeric == true) && (is_second_last_item_numeric == false) )  return 1;
      else if ( (is_last_item_numeric == true) && (is_second_last_item_numeric == true) )  return 2;
      else if ( (is_last_item_numeric == false) && (is_second_last_item_numeric == false) )  {
       
          generate_last_result_with_error_message(spring_config.getERR_MSG_NUMERIC_VALUE_EXPECTED());
          return -3; // generate error
      }

    }
    
    else if (stack_items_in_relation_to_current_operator.size() == 1){
        
        last_item_val = this.stack_items_in_relation_to_current_operator.get(stack_items_in_relation_to_current_operator.size() - 1);
        
        if (isNumeric(last_item_val)) 
            return 1;
        else { 
         
            generate_last_result_with_error_message(spring_config.getERR_MSG_NUMERIC_VALUE_EXPECTED());
            return -1;
        }    
    }
}
 catch ( NumberFormatException ex){
        
    this.error_message = spring_config.getERR_MSG_ERR_CHECK_IF_VALID_NUMBER_EXISTS_NOT_TOP_OF_STACK() + ex.toString();      
  }
 finally {}  

  return 0;

}

    public ArrayList<String> getRpn_stack() {
        return rpn_stack;
    }

    public void setRpn_stack(ArrayList<String> rpn_stack) {
        this.rpn_stack = rpn_stack;
    }

    public RPN_Calculator() {
        
        rpn_stack = new ArrayList<String>();
        
        this.command_line_items_left_of_current_operator = new ArrayList<String>();
        
        spring_context_config = new ClassPathXmlApplicationContext("SPRING_CONFIGURATION.xml");
        
        MyRpnConfig spring_config = (MyRpnConfig) spring_context_config.getBean("rpn_calc_production");
        
        this.error_message = spring_config.getInitial_message();
        

        
    }
  
}