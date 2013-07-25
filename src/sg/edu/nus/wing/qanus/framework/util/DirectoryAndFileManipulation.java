package sg.edu.nus.wing.qanus.framework.util;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;



/**
 * A mismash of useful utility functions for working with files and directories.
 * 
 * 
 * @author Ng, Jun Ping -- ngjp@nus.edu.sg
 * @version 18Sep2009
 */
public class DirectoryAndFileManipulation {
	
	
	/**
	 * Given a list of file names and the directory file in which the file names are retrieved, we want to expand the file names into absolute paths
	 * 
	 * If the input a_File is not a directory, nothing is performed.
	 * 
	 * @param a_DirectoryFileNames [in|out] the list of file names, which are not absolute file names
	 * @param a_File [in] the directory file from which the file names are retrieved
	 */
	public static void ExpandToAbsoluteFileNames(String[] a_DirectoryFileNames,
			File a_File) {
		
		// Sanity check
		if (a_DirectoryFileNames == null || a_File == null) return;
		if (!a_File.isDirectory()) return;
		
		// Expand names
		String l_DirName = a_File.getAbsolutePath();
		if (l_DirName.charAt(l_DirName.length()-1) != File.separatorChar) {
			l_DirName += File.separator;
		}
		for (int i = 0; i < a_DirectoryFileNames.length; ++i) {
			a_DirectoryFileNames[i] = l_DirName + a_DirectoryFileNames[i];			
		}
		
	} // end ExpandToAbsoluteFileNames()
	
	
	
	/**
	 * Retrieve a list of names of the files found in the given folder.
	 * 
	 * Note that sub-directories within the given folder are ignored.
	 * 
	 * The returned file names are absolute path names.
	 * 
	 * The return array is guaranteed to only contain names of files which are files, and not directories.
	 * 
	 * @param a_Folder [in] the folder we want the file listing of.
	 * @return array of strings of absolute file names of files found within the folder, or null if the folder 
	 *         is empty. null is also returned on any errors.
	 *         
	 */
	public static String[] GetListOfFilesInFolder(File a_Folder) {
		
		
		// Sanity check
		if (!a_Folder.isDirectory()) return null;
		
			
		String[] l_FileNames = a_Folder.list();
		if (l_FileNames == null) return null;				
		DirectoryAndFileManipulation.ExpandToAbsoluteFileNames(l_FileNames, a_Folder);
		
		
		// Check and weed out non-file names (ie. directory names)
		LinkedList<String> l_FinalListOfNames = new LinkedList<String>();
		for (String l_FileName : l_FileNames) {
			File l_TempFile = new File(l_FileName);
			if (!l_TempFile.isDirectory()) {
				l_FinalListOfNames.add(l_FileName);
			}
		} // end for
		
		
		if (l_FinalListOfNames.size() == 0) return null;		
		return l_FinalListOfNames.toArray(new String[0]);
		
		
	} // end GetListOfFilesInFolder()
	
	
	
	/**
	 * Given a directory, traverse all sub-directories if any, and collect all file names into a linked list.
	 * 
	 * The returned names are all absolute paths.
	 * 
	 * @param a_SourceFolder [in] the folder to traverse
	 * @return A linked list of strings of the file names of files found in the directory and sub-directories.
	 * 		   Can be null if errors are encountered, or if the directory is empty. 
	 */
	public static LinkedList<String> CollectFileNamesInDirectory(File a_SourceFolder) {
		
		
		// Sanity check
		if (a_SourceFolder == null) return null;
		if (!a_SourceFolder.isDirectory()) return null;
		
		
		// List to hold the file names
		LinkedList<String> l_ReturnList = new LinkedList<String>();
		
		
		/******** Traverse directory and sub-directories ************/
		
		// 1. Maintain a linked list of the file names we need to process
		// This is to allow us to perform the traversal iteratively instead of using
		// recursion.
		String[] l_ListOfNames = a_SourceFolder.list();		
		DirectoryAndFileManipulation.ExpandToAbsoluteFileNames(l_ListOfNames, a_SourceFolder); // Form the absolute path to the file names		
		LinkedList<String> l_NamesToProcess = new LinkedList<String>();
		DirectoryAndFileManipulation.AddStringArrayElementsToLinkedList(l_NamesToProcess, l_ListOfNames); // Place list of file names in LinkedList
		
		// 2. Start traversal, continue doing as long as there are file names in our list
		while (l_NamesToProcess.size() > 0) {
			
			// Retrieve element at head of list
			String l_FileName = l_NamesToProcess.removeFirst();
			
			// If the name is for a file, we just add it to our return list
			// If the name is for a directory, we expand the list of files in it, and add
			// them to our list of names to process
			File l_File = new File(l_FileName);
			if (l_File.isFile()) {
				l_ReturnList.add(l_FileName);
			} else if (l_File.isDirectory()) {
				String[] l_DirectoryFileNames = l_File.list();
				// Form the absolute path to the file names
				DirectoryAndFileManipulation.ExpandToAbsoluteFileNames(l_DirectoryFileNames, l_File);								
				DirectoryAndFileManipulation.AddStringArrayElementsToLinkedList(l_NamesToProcess, l_DirectoryFileNames);
			} else {
				// Unknown type, we'd skip it
				// TODO use a verbose flag to condition this println
				System.err.println("DirectoryAndFileManipulation::CollectFileNamesInDirectory() >> Error processing [" + l_FileName + "]");
			}
			
			
		} // end while
		
			
		// To simplify the return param, if the list is empty we nullify it, 
		// of course trusting the garbage collector to reclaim the memory used.
		if (l_ReturnList.size() == 0) l_ReturnList = null;
		
		return l_ReturnList;
		
	} // end CollectFileNamesInDirectory()
	
	
	/**
	 * Given an array of strings, we want to add each element into a given linked list.
	 * The elements are added to the rear of the list.
	 * 
	 * @param a_StringsToProcess [out] the linked list we want to add to
	 * @param a_ListOfStrings [in] the array of strings
	 */
	private static void AddStringArrayElementsToLinkedList(
			LinkedList<String> a_StringsToProcess, String[] a_ListOfStrings) {
		
		// Sanity check
		if (a_StringsToProcess == null) return;
		if (a_ListOfStrings == null) return;
		
		for (String l_Name : a_ListOfStrings) {
			a_StringsToProcess.addLast(l_Name);
		}
		
	} // end AddStringArrayElementsToLinkedList()



	/**
	 * Creates a specified directory if it does not exist.
	 * If it already exists, nothing is done.
	 * 
	 * If the specified directory is given as a relative path name, it would be created
	 * relative to the current working directory.
	 *
	 * If the specified directory has nested sub-directories, each directory will be created
	 * in turn.
	 * 
	 * @param a_TargetFolder [in] the target directory to create.
	 * @return true if the directory is created successfully, or already exists. false if the directory does not exist
	 *         and we fail to create it.
	 */
	public static boolean CreateDirectoryIfNonExistent(File a_TargetFolder) {
		
				
		// Recursively create all required directories
		if (a_TargetFolder.getPath().lastIndexOf(File.separatorChar) != -1) {
			int l_IndexToCut = a_TargetFolder.getPath().lastIndexOf(File.separatorChar);
			File l_RecursiveFile = new File(a_TargetFolder.getPath().substring(0, l_IndexToCut));
			CreateDirectoryIfNonExistent(l_RecursiveFile);
		}

		
		// Check whether the given folder is expressed as an absolute path name
		if (!a_TargetFolder.isAbsolute()) {
		
			// The folder is only expressed as a relative path name.
			// We'd expand it into the absolute path.
			
			// 	Obtain the current working directory to house the created directory.
			String l_CurrentDir = System.getProperty("user.dir");
			File l_CurrentDirFile = new File(l_CurrentDir);		

			a_TargetFolder = new File(l_CurrentDirFile.toString() + File.separatorChar + a_TargetFolder.getPath() + File.separatorChar);
		
		}
		
		
		if (!a_TargetFolder.exists()) {
			// Ensure that it exists
			return a_TargetFolder.mkdir();
		} else {
			if (a_TargetFolder.isDirectory())
				return true;
			else
				return false;
		}
		
		
	} // end CreateDirectoryIfNonExistent()

	
	/**
	 * Creates a file if it does not already exist, or wipe it clean if it already does.
	 * We expect the file name to be an absolute path name.
	 * 
	 * @param a_FileName [in] name of the file to create
	 * @return File pointer to the created temp file, or null on any errors;
	 */
	private File CreateNewFile(String a_AbsoluteFileName)  {
				
		File l_TempFile = new File(a_AbsoluteFileName);
		try {
			if (!l_TempFile.createNewFile()) {  // If !createNewFile() means file already exist
				// Since file already exist, we delete it off, and start afresh
				l_TempFile.delete();
				l_TempFile.createNewFile();
			}
		} catch (IOException e) {			
			return null;
		}
		
		return l_TempFile;
		
	} // end CreateNewFile()
	
	
} // end class
