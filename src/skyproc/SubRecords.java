/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package skyproc;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.zip.DataFormatException;
import lev.LChannel;
import lev.LExporter;
import lev.LFileChannel;
import lev.LChannel;
import skyproc.exceptions.BadParameter;
import skyproc.exceptions.BadRecord;

/**
 *
 * @author Justin Swanson
 */
class SubRecords implements Iterable<SubRecord>, Serializable {

    private ArrayList<SubRecord> list = new ArrayList<>();
    private Map<Type, SubRecord> map = new HashMap<>(0);
    private Map<Type, Long> pos = new HashMap<>(0);
    private Set<Type> forceExport = new HashSet<>(0);

    public void add(SubRecord r) {
	for (Type t : r.getTypes()) {
	    map.put(t, r);
	}
	list.add(r);
    }

    void export(LExporter out, Mod srcMod) throws IOException {
	for (SubRecord s : list) {
	    if (shouldExport(s)) {
		s.export(out, srcMod);
	    }
	}
    }

    public void forceExport(Type t) {
	forceExport.add(t);
    }

    public boolean contains(Type t) {
	return map.containsKey(t);
    }

    public SubRecord get(Type in) {
	return map.get(in);
    }

    boolean isValid() {
	for (SubRecord s : list) {
	    if (!s.isValid()) {
		return false;
	    }
	}
	return true;
    }

    void printSummary() {
	if (SPGlobal.logging() && SPGlobal.debugSubrecordSummary) {
	    String header = "Summary: ";
	    String data = "";
	    int counter = 0;
	    ArrayList<Type> printedTypes = new ArrayList<Type>();
	    for (Type type : getTypes()) {
		SubRecord s = get(type);
		if (s.isValid() && !printedTypes.contains(type)) {
		    data = data + type.toString() + " ";
		    if (s instanceof SubList) {
			data = data + "(" + ((SubList) s).size() + ") ";
		    }
		    printedTypes.addAll(Arrays.asList(s.getTypes()));
		    if (counter++ == 12) {
			SPGlobal.logSync("Subrecords", header + data);
			header = "-------- ";
			data = "";
			counter = 0;
		    }
		}
	    }
	    if (counter > 0) {
		SPGlobal.logSync("Subrecords", header + data);
	    }
	}
    }

    void importSubRecords(LChannel in) throws BadRecord, BadParameter, DataFormatException {
	while (!in.isDone()) {
	    importSubRecord(in);
	}
    }

    void importSubRecord(LChannel in) throws BadRecord, DataFormatException, BadParameter {
	Type nextType = Record.getNextType(in);
	if (contains(nextType)) {
//	    if (in.getClass().equals(LFileChannel.class)) {
//		pos.put(nextType, ((LFileChannel) in).pos());
//	    } else {
		SubRecord record = get(nextType);
		record.parseData(record.extractRecordData(in));
//	    }
	} else {
	    throw new BadRecord("Doesn't know what to do with a " + nextType.toString() + " record.");
	}
    }

    public void remove(Type in) {
	if (map.containsKey(in)) {
	    for (int i = 0; i < list.size(); i++) {
		if (list.get(i).getTypes()[0].equals(in)) {
		    list.remove(i);
		    break;
		}
	    }
	    map.remove(in);
	    pos.remove(in);
	    forceExport.remove(in);
	}
    }

    public int length(Mod srcMod) {
	int length = 0;
	for (SubRecord s : list) {
	    if (shouldExport(s)) {
		length += s.getTotalLength(srcMod);
	    }
	}
	return length;
    }

    public boolean shouldExport(SubRecord s) {
	return s.isValid() || forceExport.contains(s.getTypes()[0]);
    }

    public ArrayList<SubRecord> getRecords() {
	return new ArrayList(map.values());
    }

    public Set<Type> getTypes() {
	return map.keySet();
    }

    void fetchStringPointers(Mod srcMod, Record r, Map<SubStringPointer.Files, LChannel> streams) {
	for (SubRecord s : list) {
	    s.fetchStringPointers(srcMod, r, streams);
	}
    }

    public ArrayList<FormID> allFormIDs() {
	ArrayList<FormID> out = new ArrayList<FormID>();
	for (SubRecord s : list) {
	    out.addAll(s.allFormIDs());
	}
	return out;
    }

    @Override
    public Iterator<SubRecord> iterator() {
	return list.iterator();
    }
}