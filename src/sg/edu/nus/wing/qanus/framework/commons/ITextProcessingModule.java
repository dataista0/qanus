package sg.edu.nus.wing.qanus.framework.commons;


/**
 * Interface that text processing modules should implement.
 * This interface allows them to take in input text and returns
 * annotated strings of the input text.
 *
 * @author NG, Jun Ping -- ngjp@nus.edu.sg
 * @since 18Sep2009
 */
public interface ITextProcessingModule extends IRegisterableModule {

	/**
	 * Call-back function which will be invoked by the StageEngine when
	 *  a new data item is parsed from the source documents.
	 * This function will then process the input text and retun the annotated text.
	 * 
	 * @param a_Sentences [in] the data item to be annotated.
	 * @return array of annotated sentences, with a 1-1 correspondence to the input array
	 */	
	public String[] ProcessText(String[] a_Sentences);
	
	
} // end interface
