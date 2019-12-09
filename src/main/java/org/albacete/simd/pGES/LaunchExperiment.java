package org.albacete.simd.pGES;

public class LaunchExperiment {

	
	public static void main(String[] args) throws Exception {
		// Reading parameters
		int net_number = Integer.parseInt(args[0]);
		int bbdd_number = Integer.parseInt(args[1]);
		int fusion_number = Integer.parseInt(args[2]);
		int nThreads = Integer.parseInt(args[3]);
		int nItInterleaving = Integer.parseInt(args[4]);
		
		// Processing parameters
		String net_path;
		String net_name;
		String bbdd_path;
		String fusion_consensus;
		switch(net_number){
			case 0:
				net_path = "networks/alarm.xbif";
				net_name = "alarm";
				break;
			case 1:
				net_path = "networks/andes.xbif";
				net_name = "andes";
				break;
			case 2:
				net_path = "networks/barley.xbif";
				net_name = "barley";
				break;
			case 3:
				net_path = "networks/cancer.xbif";
				net_name = "cancer";
				break;
			case 4:
				net_path = "networks/child.xbif";
				net_name = "child";
				break;
			case 5:
				net_path = "networks/earthquake.xbif";
				net_name = "earthquake";
				break;
			case 6:
				net_path = "networks/ejemplo.xbif";
				net_name = "ejemplo";
				break;
			case 7:
				net_path = "networks/hailfinder.xbif";
				net_name = "hailfinder";
				break;
			case 8:
				net_path = "networks/hepar2.xbif";
				net_name = "hepar2";
				break;
			case 9:
				net_path = "networks/insurance.xbif";
				net_name = "insurance";
				break;
			case 10:
				net_path = "networks/link.xbif";
				net_name = "kink";
				break;
			case 11:
				net_path = "networks/mildew.xbif";
				net_name = "mildew";
				break;
			case 12:
				net_path = "networks/munin.xbif";
				net_name = "munin";
				break;
			case 13:
				net_path = "networks/pigs.xbif";
				net_name = "pigs";
				break;
			case 14:
				net_path = "networks/water.xbif";
				net_name = "water";
				break;
			case 15:
				net_path = "networks/win95pts.xbif";
				net_name = "win95pts";
				break;
			default:
				System.out.println("Incorrect net number");
				throw new Exception();
		}
		
		switch(bbdd_number){
			case 0:
				bbdd_path = "networks/BBDD/" + net_name + ".xbif" + "_.csv";
				break;
			case 1:
				bbdd_path = "networks/BBDD/" + net_name + ".xbif50000" + "_.csv";
				break;
			case 2:
				bbdd_path = "networks/BBDD/" + net_name + ".xbif50001" + "_.csv";
				break;
			case 3:
				bbdd_path = "networks/BBDD/" + net_name + ".xbif50001246" + "_.csv";
				break;
			case 4:
				bbdd_path = "networks/BBDD/" + net_name + ".xbif50002" + "_.csv";
				break;
			case 5:
				bbdd_path = "networks/BBDD/" + net_name + ".xbif50003" + "_.csv";
				break;
			case 6:
				bbdd_path = "networks/BBDD/" + net_name + ".xbif50004" + "_.csv";
				break;
			case 7:
				bbdd_path = "networks/BBDD/" + net_name + ".xbif50005" + "_.csv";
				break;
			case 8:
				bbdd_path = "networks/BBDD/" + net_name + ".xbif50006" + "_.csv";
				break;
			case 9:
				bbdd_path = "networks/BBDD/" + net_name + ".xbif50007" + "_.csv";
				break;
			case 10:
				bbdd_path = "networks/BBDD/" + net_name + ".xbif50008" + "_.csv";
				break;
			case 11:
				bbdd_path = "networks/BBDD/" + net_name + ".xbif500009" + "_.csv";
				break;
			default:
				System.out.println("Incorrect bbdd number");
				throw new Exception();
			
		}
		
		switch(fusion_number) {
		case 0:
			fusion_consensus = "ConsensusBES";
			break;
		case 1:
			fusion_consensus = "HeuristicConsensusMVoting";
			break;
		default:
			System.out.println("Incorrect bbdd number");
			throw new Exception();
		}
		
		// Total of 3456 experiments
		// Running Experiment
		Experiments experiment = new Experiments(net_path, bbdd_path, fusion_consensus, nThreads, nItInterleaving);
		experiment.runExperiment();
		experiment.saveExperiment();
	}
}
