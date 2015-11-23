package wtr.g6;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class WisdomComparator implements Comparator<Person>{

	// In the future, add more attributes here as we make this logic more sophisticated!

    @Override
    public int compare(Person p1, Person p2) {
    	//System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
    	if(p1.wisdom > p2.wisdom) return -1;
    	else if(p1.wisdom < p2.wisdom) return 1;
    	else return 0;
        //return p1.wisdom > p2.wisdom ? -1 : 1;
    }
}