package nationGen;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import com.elmokki.Dom3DB;
import com.elmokki.Drawing;
import com.elmokki.Generic;

import nationGen.entities.Entity;
import nationGen.entities.Filter;
import nationGen.entities.Flag;
import nationGen.entities.MagicItem;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.entities.AbilityTemplate;
import nationGen.entities.Theme;
import nationGen.items.CustomItem;
import nationGen.items.Item;
import nationGen.magic.MagicPattern;
import nationGen.misc.Command;
import nationGen.misc.ItemSet;
import nationGen.misc.ResourceStorage;
import nationGen.misc.Site;
import nationGen.naming.EpithetGenerator;
import nationGen.naming.MageNamer;
import nationGen.naming.NameFilter;
import nationGen.naming.NameGenerator;
import nationGen.naming.NationAdvancedSummarizer;
import nationGen.naming.NationDescriber;
import nationGen.naming.PriestNamer;
import nationGen.naming.SacredNamer;
import nationGen.naming.TroopNamer;
import nationGen.nation.Nation;
import nationGen.units.ShapeChangeUnit;
import nationGen.units.ShapeShift;
import nationGen.units.Unit;



public class NationGen {
	public static String version = "0.5.0.11E";
	public static String date = "29th of May 2015";
	
	public ResourceStorage<MagicPattern> patterns = new ResourceStorage<MagicPattern>(MagicPattern.class, this);
	public ResourceStorage<Pose> poses = new ResourceStorage<Pose>(Pose.class, this);
	public ResourceStorage<Filter> filters = new ResourceStorage<Filter>(Filter.class, this);
	public ResourceStorage<NameFilter> magenames = new ResourceStorage<NameFilter>(NameFilter.class, this);
	public ResourceStorage<Filter> miscdef = new ResourceStorage<Filter>(Filter.class, this);
	public ResourceStorage<Flag> flagparts = new ResourceStorage<Flag>(Flag.class, this);
	public ResourceStorage<MagicItem> magicitems = new ResourceStorage<MagicItem>(MagicItem.class, this);
	public ResourceStorage<NameFilter> miscnames = new ResourceStorage<NameFilter>(NameFilter.class, this);
	public ResourceStorage<AbilityTemplate> templates = new ResourceStorage<AbilityTemplate>(AbilityTemplate.class, this);
	public ResourceStorage<Filter> descriptions = new ResourceStorage<Filter>(Filter.class, this);
	public ResourceStorage<Pose> monsters = new ResourceStorage<Pose>(Pose.class, this);
	public ResourceStorage<Theme> themes = new ResourceStorage<Theme>(Theme.class, this);

	public List<String> secondShapeMountCommands = new ArrayList<String>();
	public List<String> secondShapeNonMountCommands = new ArrayList<String>();
	public List<String> secondShapeRacePoseCommands = new ArrayList<String>();

	public Dom3DB weapondb;
	public Dom3DB armordb;
	public Dom3DB units;
	public Dom3DB sites;
	public Dom3DB nations;

	public Settings settings;
	public List<CustomItem> customitems = new ArrayList<CustomItem>();
	public List<ShapeShift> secondshapes = new ArrayList<ShapeShift>();
	public List<Race> races = new ArrayList<Race>();
	public IdHandler idHandler;
	
	public List<ShapeChangeUnit> forms = new ArrayList<ShapeChangeUnit>();
	public List<CustomItem> chosenCustomitems = new ArrayList<CustomItem>();
	public List<CustomItem> pickedCustomitems = new ArrayList<CustomItem>();
	
	public NationGen() throws IOException
	{
       

		
		
        //System.out.println("Dominions 4 NationGen version " + version + " (" + date + ")");
        //System.out.println("------------------------------------------------------------------");

        System.out.print("Loading settings... ");
		settings = new Settings();
		System.out.println("done!");
		

		try {
	        System.out.print("Loading Edi's Dom4DB... ");
			loadDom3DB();
			System.out.println("done!");
	        System.out.print("Loading definitions... ");
			customitems.addAll(Item.readFile(this, "./data/items/customitems.txt", CustomItem.class));
			patterns.load("./data/magic/magicpatterns.txt");
			poses.load("./data/poses/poses.txt");
			filters.load("./data/filters/filters.txt");
			magenames.load("./data/names/magenames/magenames.txt");
			miscnames.load("./data/names/naming.txt");
			templates.load("./data/templates/templates.txt");
			descriptions.load("./data/descriptions/descriptions.txt");
			themes.load("./data/themes/themes.txt");
			
			monsters.load("./data/monsters/monsters.txt");
			loadRaces("./data/races/races.txt");
			secondshapes = Entity.readFile(this, "./data/shapes/secondshapes.txt", ShapeShift.class);
			miscdef.load("./data/misc/miscdef.txt");
			flagparts.load("./data/flags/flagdef.txt");
			magicitems.load("./data/items/magicweapons.txt");
			
			
			loadSecondShapeInheritance("/data/shapes/secondshapeinheritance.txt");
			
			System.out.println("done!");

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error loading file " + e.getMessage());
		}
		
		
		for(CustomItem ci : this.customitems)
			if(ci.armor)
				this.armordb.addToMap(ci.name, ci.getHashMap());
			else
				this.weapondb.addToMap(ci.name, ci.getHashMap());

		
		
		//this.writeDebugInfo();
	}
	
	public int seed = 0;
	public String modname = "";
	public boolean manyseeds = false;

	public void generate(int amount) throws IOException
	{
		Random random = new Random();
		generate(amount, random.nextInt(), null);
	}
	
	public void generate(int amount, int seed) throws IOException
	{
		generate(amount, seed, null);
	}
	
	public void generate(List<Integer> seeds) throws IOException
	{
		Random random = new Random();
		generate(1, random.nextInt(), seeds);
	}
	
	private void generate(int amount, int seed, List<Integer> seeds) throws IOException
	{
		this.seed = seed;

		Random random = new Random(seed);

		// If there's a list of seeds.
		if(seeds != null && seeds.size() > 0)
		{	
			manyseeds = true;
			amount = seeds.size();
			random = new Random(0);
		}

		// Start
		idHandler = new IdHandler();
		idHandler.loadFile("forbidden_ids.txt");
        

		
		if(!manyseeds)
			System.out.println("Generating " + amount + " nations with seed " + seed + ".");
		else
			System.out.println("Generating " + amount + " nations with predefined seeds.");

		
		System.out.print("Generating nations");
		List<Nation> nations = new ArrayList<Nation>();
		for(int i = 0; i < amount; i++)
		{
			int newseed = 0;
			if(!manyseeds)
				newseed = random.nextInt();
			else
				newseed = seeds.get(i);
			
			Nation n = new Nation(this, idHandler.nextNationId(), newseed);

			n.name = "Nation " + (i + 1);
			nations.add(n);
			System.out.print(".");
		}
		System.out.println(" Done!");

		

		System.out.print("Giving ids");
		for(Nation n : nations)
		{
			
			
			// units
			for(List<Unit> ul : n.unitlists.values())
				for(Unit u : ul)
				{
					if(!u.invariantMonster)
					{
						u.id = idHandler.nextUnitId();
					}
					// Else the monster's ID was set in MonsterGen
				}	
			
			for(List<Unit> ul : n.comlists.values())
				for(Unit u : ul)
				{
					u.id = idHandler.nextUnitId();
				}	
			
			for(Unit u : n.heroes)
			{
				u.id = idHandler.nextUnitId();
			}
			
	
			
			// sites
			for(Site s : n.sites)
			{
				s.id = idHandler.nextSiteId();
			}
			
			

			
			System.out.print(".");		
		}
		System.out.println(" Done!");

		
		System.out.print("Naming things");
		
		NameGenerator nGen = new NameGenerator(this);
		for(Nation n : nations)
		{
			n.name = nGen.generateNationName(n.races.get(0), n);
			n.nationalitysuffix = nGen.getNationalitySuffix(n, n.name);

			
			// troops
			TroopNamer tnamer = new TroopNamer(n);
			tnamer.execute();
			
			// sites
			for(Site s : n.sites)
				s.name = nGen.getSiteName(n.random, s.getPath(), s.getSecondaryPath());
	
			// mages 
			MageNamer mNamer = new MageNamer(n);
			mNamer.execute();
			
			// priests
			PriestNamer pNamer = new PriestNamer(n);
			pNamer.execute();
			
			// sacreds and elites
			SacredNamer sNamer = new SacredNamer(n);
			sNamer.nameSacreds(n);

			// Epithet
			EpithetGenerator epiGen = new EpithetGenerator(this);
			epiGen.giveEpithet(n);
			
			// Unit descriptions
			new NationDescriber(n);
			
			// Summaries
			n.summary.update();
			
			System.out.print(".");		
		}
		

		
		// Get mod name if not custom
		if(modname.equals(""))
		{
			
			if(nations.size() > 1)
				modname = nGen.getSiteName(nations.get(0).random, nations.get(0).random.nextInt(8), nations.get(0).random.nextInt(8));
			else
				modname = nations.get(0).name;
		}
		
		System.out.println(" Done!");

		
		String filename = modname.replaceAll(" ", "_").toLowerCase();
		try {
			this.write(nations, filename);
		} catch (IOException e) {
			System.out.println("Error writing mod: " + e.getMessage());
		}
		
        System.out.println("------------------------------------------------------------------");
        System.out.println("Finished generating " + amount + " nations to file nationgen_" + filename + ".dm!");
	
        seed = 0;
        modname = "";
	}
	

	
	/**
	 * Loads data from Dom3DB
	 */
	private void loadDom3DB() throws Exception
	{
		units = new Dom3DB("units.csv");
		armordb = new Dom3DB("armor.csv");
		weapondb = new Dom3DB("weapon.csv");
		sites = new Dom3DB("sites.csv");
		nations = new Dom3DB("nations.csv");
	}
	
	


	private void loadRaces(String file) throws IOException
	{
		FileInputStream fstream = new FileInputStream(file);
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		
		String strLine;
		
		
		while ((strLine = br.readLine()) != null)   
		{
			List<String> args = Generic.parseArgs(strLine);
			if(args.size() == 0)
				continue;
			
			if(args.get(0).equals("#load"))
			{
				List<Race> items = new ArrayList<Race>();
				items.addAll(Item.readFile(this, args.get(1), Race.class));
				races.addAll(items);
			}
		}
		

		
		in.close();
		
		
	}

	
	public void writeDebugInfo()
	{
		double total = 0;
		for(Race r : races)
		{
			if(!r.tags.contains("secondary"))
				total += r.basechance;
		}
		
		for(Race r : races)
			if(!r.tags.contains("secondary"))
				System.out.println(r.name + ": " + (r.basechance / total));
	}

	
	public void write(List<Nation> nations, String modname) throws IOException
	{
		
		String dir = "nationgen_" + modname.toLowerCase().replaceAll(" ", "_") + "/"; // nation.name.toLowerCase().replaceAll(" ", "_")
		new File("./mods/" + dir).mkdir();
		
		FileWriter fstream = new FileWriter("./mods/nationgen_" + modname.toLowerCase().replaceAll(" ", "_") + ".dm");
		PrintWriter tw = new PrintWriter(fstream);
		
		// Descriptions
		NationAdvancedSummarizer nDesc = new NationAdvancedSummarizer(armordb, weapondb);
		if(settings.get("advancedDescs") == 1.0)
			nDesc.writeAdvancedDescriptionFile(nations, modname);
		if(settings.get("basicDescs") == 1.0)
			nDesc.writeDescriptionFile(nations, modname);
		
		// Description!
		tw.println("-- NationGen - " + modname);
		tw.println("-----------------------------------");
		
		tw.println("-- Generated with version " + version + ".");
		tw.println("-- Generation setting code: " + settings.getSettingInteger());

		if(!manyseeds)
			tw.println("-- Nation seeds generated with seed " + this.seed + ".");
		else
			tw.println("-- Nation seeds specified by user.");
		

	
		for(Nation n : nations)
			tw.println("-- Nation " + n.nationid + ": " + n.name + " generated with seed " + n.seed);
		tw.println("-----------------------------------");
		tw.println();
		
		// Actual mod definition
        tw.println("#modname \"NationGen - " + this.modname + "\"");
        tw.println("#description \"A NationGen generated nation!\"");
        
        // Banner!
        generateBanner(nations.get(0).colors[0], this.modname, dir + "/banner.tga", nations.get(0).flag);
        tw.println("#icon \"" + dir + "banner.tga\"");
        tw.println("");
        

        
        // Write items!
        // This is a relic from Dom3 version, but oh well.
		System.out.print("Writing items");
		this.writeCustomItems(tw);
        for(Nation nation : nations)
        {
			System.out.print(".");

        }

		System.out.println(" Done!");
	
		
        
      
	
        
		// Write units!
		System.out.print("Writing units");
        for(Nation nation : nations)
        {
        	new File("./mods/" + dir + "/" + nation.nationid + "-" + nation.name.toLowerCase().replaceAll(" ", "_") + "/").mkdir();

	        // Unit definitions
	        nation.writeUnits(tw, dir + "/" + nation.nationid + "-" + nation.name.toLowerCase().replaceAll(" ", "_") + "/");
	        
			System.out.print(".");

        }
		System.out.println(" Done!");
		
		
		// Write sites!
		System.out.print("Writing sites");
        for(Nation nation : nations)
        {
	        // Site definitions
	        nation.writeSites(tw);
	        
			System.out.print(".");

        }
		System.out.println(" Done!");
      
		
		// Write nation definitions!
		System.out.print("Writing nations");
        for(Nation nation : nations)
        {
        	// Flag
        	Drawing.writeTGA(nation.flag, "mods/" + dir + "/" + nation.nationid + "-" + nation.name.toLowerCase().replaceAll(" ", "_") + "/flag.tga");

	        // Nation definitions
	        nation.write(tw, dir + "/" + nation.nationid + "-" + nation.name.toLowerCase().replaceAll(" ", "_") + "/");
			System.out.print(".");
        }
		System.out.println(" Done!");
        
		
		if(settings.get("hidevanillanations") == 1)
			hideVanillaNations(tw, nations.size());
		
        tw.flush();
        tw.close();
        
        // Displays mage names
        /*
        System.out.println();
        for(Nation n : nations)
        {
        	List<Unit> mages = n.generateComList("mage");
        	List<String> mnames = new ArrayList<String>();
        	for(Unit u : mages)
        		mnames.add(u.name.toString());
        	System.out.println("* " + Generic.listToString(mnames, ","));
        			
        }
        System.out.println();
       */
	}
	
	private void hideVanillaNations(PrintWriter tw, int nationcount)
	{

		System.out.print("Hiding vanilla nations... ");
		
		tw.println("-- Hiding vanilla nations");
		tw.println("-----------------------------------");
		
		
		if(nationcount > 1)
		{
			tw.println("#disableoldnations");
			tw.println();
			System.out.println(" Done!");
		}
		else
			System.out.println("Unable to hide vanilla nations with only one random nation!");
		
	}
	
	
	public void writeCustomItems(PrintWriter tw)
	{
		if(chosenCustomitems.size() == 0)
			return;

		tw.println("--- Generic custom items.");
		for(CustomItem ci : this.chosenCustomitems)
		{	
			ci.write(tw);
			//tw.println("");
		}
	}
	
	public CustomItem getCustomItem(String name)
	{
		CustomItem citem = null;
		for(CustomItem ci : this.customitems)
		{
			if(ci.name.equals(name) && !this.chosenCustomitems.contains(ci))
			{
				citem = ci;
				break;
			}
		}
		
		return citem;
	}
	
	public String getCustomItemId(String name)
	{
		
		for(CustomItem ci : this.chosenCustomitems)
			if(ci.name.equals(name))
			{
				return ci.id;
			}
		
		
		CustomItem citem = null;
		for(CustomItem ci : this.customitems)
		{
			if(ci.name.equals(name) && !chosenCustomitems.contains(ci))
			{
				citem = ci.getCopy();
				break;
			}
		}
		
		if(citem == null)
		{
			System.out.println("WARNING: No custom item named " + name + " was found!");
			return "-1";
		}
		
		if(idHandler != null)
		{
			if(citem.armor)
				citem.id = idHandler.nextArmorId() + "";
			else
				citem.id = idHandler.nextWeaponId() + "";
		}
		else
		{
			System.out.println("ERROR: idHandler was not initialized!");
			citem.id = "-1";
		}
		
		// -521978361
		// Check references!
		for(String str : citem.values.keySet())
		{
			if(str.equals("secondaryeffect") || str.equals("secondaryeffectalways"))
			{
				try
				{
					Integer.parseInt(citem.values.get(str));
				}
				catch(Exception e)
				{
					String id = getCustomItemId(citem.values.get(str));
					citem.values.put(str, id);
				}
			}
		}
		
		
		this.chosenCustomitems.add(citem);
		//this.customitems.remove(citem);
		
		
		if(!citem.armor)
			weapondb.addToMap(citem.id, citem.getHashMap());
		else
			armordb.addToMap(citem.id, citem.getHashMap());
		
		return citem.id;

	}


	public boolean hasShapeShift(String id)
	{
		int realid = -1;
		try
		{
			realid = Integer.parseInt(id);
		}
		catch(Exception e)
		{
			return false;
		}
	
		for(ShapeChangeUnit su : this.forms)
			if(su.id == realid)
				return true;
		
		return false;
	}
	
	public void handleShapeshift(Command c, Unit u)
	{		
		ShapeShift shift = null;
		for(ShapeShift s : secondshapes)
		{
			if(s.name.equals(c.args.get(0)))
			{
				shift = s;
				break;
			}
		}
		
		if(shift == null)
		{
			System.out.println("Shapeshift named " + c.args.get(0) + " could not be found.");
			return;
		}
		ShapeChangeUnit su = new ShapeChangeUnit(this, u.race, u.pose, u, shift);
		
		su.id = idHandler.nextUnitId();
		
		if(c.command.equals("#shapechange"))
		{
			su.shiftcommand = "#shapechange";
		}
		else if(c.command.equals("#secondshape"))
		{
			su.shiftcommand = "#firstshape";
		}
		else if(c.command.equals("#firstshape"))
		{
			su.shiftcommand = "#secondshape";
		}
		else if(c.command.equals("#secondtmpshape"))
		{
			su.shiftcommand = "";
		}
		else if(c.command.equals("#landshape"))
		{
			su.shiftcommand = "#watershape";
		}
		else if(c.command.equals("#watershape"))
		{
			su.shiftcommand = "#landshape";
		}
		else if(c.command.equals("#forestshape"))
		{
			su.shiftcommand = "#plainshape";
		}
		else if(c.command.equals("#plainshape"))
		{
			su.shiftcommand = "#forestshape";
		}
		
		c.args.set(0, "" + su.id);
		forms.add(su);

	}
	
	


	/**
	 * Loads the list of commands that second shapes should inherit from the primary shape
	 * @param filename
	 * @return
	 */
	public int loadSecondShapeInheritance(String filename)
	{
		int amount = 0;
		
        Scanner file;
		
		try {
			file = new Scanner(new FileInputStream(System.getProperty("user.dir") + "/" + filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return 0;
		}

		while(file.hasNextLine())
		{
			String line = file.nextLine();
			if(line.startsWith("-"))
				continue;
			
			List<String> args = Generic.parseArgs(line);
			if(args.size() == 0)
				continue;
			
			if(args.get(0).equals("all") && args.size() > 0)
			{
				secondShapeMountCommands.add(args.get(1));
				secondShapeNonMountCommands.add(args.get(1));
				amount++;
			}
			else if(args.get(0).equals("mount") && args.size() > 0)
			{
				secondShapeMountCommands.add(args.get(1));
				amount++;
			}
			else if(args.get(0).equals("nonmount") && args.size() > 0)
			{
				secondShapeNonMountCommands.add(args.get(1));
				amount++;
			}
			else if(args.get(0).equals("racepose") && args.size() > 0)
			{
				secondShapeRacePoseCommands.add(args.get(1));
				amount++;
			}
			
		}
		file.close();
		
		return amount;
	}
	
	

	public static void generateBanner(Color c, String name, String output, BufferedImage flag) throws IOException
	{
		BufferedImage combined = new BufferedImage(256, 64, BufferedImage.TYPE_INT_RGB);
		Graphics g = combined.getGraphics();
		
		g.setColor(new Color(0, 0, 0));
		g.fillRect(0, 0, 256, 64);
		g.drawImage(flag, 0, -4, null);
		

		
	
		g.setColor(Color.DARK_GRAY);
		
		Font f = g.getFont();
		Font d = f.deriveFont(20f);
		f = f.deriveFont(24f);
		g.setFont(d);
		
		g.drawString("NationGen:", 64, 24);

		
		g.setFont(f);
		g.drawString(name, 64, 56);


		
		Drawing.writeTGA(combined, "./mods/" + output);
	
	}
}