package i5.las2peer.services.mentoringCockpitService.Themes;

import java.util.ArrayList;
import java.util.Iterator;

import i5.las2peer.services.mentoringCockpitService.Model.Resources.Resource;

public class ThemeResourceLink {
	private Resource linkedResource;
	private ArrayList<Information> linkInformation;
	
	public ThemeResourceLink(Resource resource) {
		this.linkedResource = resource;
		this.linkInformation = new ArrayList<Information>();
		
	}
	
	public void addInfo(String infoType, String infoVal) {
		//System.out.println("DEBUG --- Info: " + infoType + " " + infoVal);
		if (infoType.contains("pagerange")) {
			linkInformation.add(new Pages(infoVal));
		} else if (infoType.contains("timestamp")) {
			linkInformation.add(new Timestamp(infoVal));
		}
	}
	
	public String getSuggestionText() {
		String resultString = linkedResource.getSuggestionItemText() + " (";
		Iterator<Information> iterator = linkInformation.iterator();
		while (iterator.hasNext()) {
			resultString = resultString + iterator.next().getInfoText();
			if (iterator.hasNext()) {
				resultString = resultString + ", ";
			}
		}
		
		return resultString + ")";
	}
}