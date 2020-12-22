import os
import re
import sys
from xml.dom import minidom

ORIGINAL_NETWORK_FOLDER = "/Users/jdls/developer/projects/ParallelBNs/res/networks/"
REPLICATE_NETWORK_FOLDER = "../res/networks/replicates/"
NETWORKS = ["alarm", "cancer", "mildew", "barley", "child", "insurance", "water"]
CLONES = [2,4,6,8]


def getNetworkPaths():
    p = re.compile(r'\.xbif$')
    paths = []
    for root, dirs, files in os.walk(ORIGINAL_NETWORK_FOLDER):
        #for d in dirs:
        #    print(os.path.relpath(os.path.join(root, d), "."))
        for f in files:
            is_xbif = p.search(f)
            if (is_xbif and 'X' not in f):
                print(os.path.relpath(os.path.join(root, f), "."))
                paths.append(os.path.relpath(os.path.join(root, f), "."))
    return paths

def getNetNameFromPath(path):
    regex = re.compile(r'.*/(.*)\.xbif$')
    result = regex.search(path)
    if result:
        return result.group(1)
    return None

def replicate(xbif_file, nclones, save_path):
    # parse an xml file by name
    mydoc = minidom.parse(xbif_file)

    # Getting elements from xml file
    variables = mydoc.getElementsByTagName('VARIABLE')
    prob_tables = mydoc.getElementsByTagName("DEFINITION")
    network = mydoc.getElementsByTagName("NETWORK")[0]

    # Cloning and adding variables to the network
    for elem in variables:
        for i in range(1, nclones):
            clone = elem.cloneNode(deep=True)
            clone_name = clone.getElementsByTagName("NAME")[0].firstChild
            clone_name.data = clone_name.data +"_" + str(i)
            network.appendChild(clone)

    # Cloning Prob_tables
    for table in prob_tables:
        for i in range(1, nclones):
            clone = table.cloneNode(deep=True)
            for_node = clone.getElementsByTagName("FOR")[0].firstChild
            for_node.data = for_node.data + "_" + str(i)
            given_nodes = clone.getElementsByTagName("GIVEN")
            if given_nodes:
                for elem in given_nodes:
                    node = elem.firstChild
                    node.data = node.data + "_" + str(i)
            #print(clone.toprettyxml())
            network.appendChild(clone)   


    # Saving new Xml file
    #print(mydoc.toprettyxml())
    #save_path = '../res/networks/replicates/' + str(nclones) +'Xalarm.xbif'
    with open(save_path, "w") as f:
        f.write(mydoc.toprettyxml())

if __name__ == "__main__":
    paths = getNetworkPaths()
    for xbif_file in paths:
        net = getNetNameFromPath(xbif_file)
        for nclone in CLONES:
            output_path = REPLICATE_NETWORK_FOLDER + str(nclone) + "X" + net + ".xbif" 
            #print("Xbif file: " + xbif_file + "\tNClones: " + str(nclone) + "\tOutput File: " + output_path)
            replicate(xbif_file, nclone, output_path)
            
    # xbif_files = [ORIGINAL_NETWORK_FOLDER + net + ".xbif" for net in NETWORKS]
    # replicate(xbif_file, nclones, output_file)
    # print(xbif_file + " has been replicated "+ nclones + " times and saved as " + output_file)