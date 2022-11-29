import os
import sys
from xml.dom import minidom

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
    xbif_file = sys.argv[0]
    nclones = int(sys.argv[1])
    output_file = sys.argv[2]
    replicate(xbif_file, nclones, output_file)
    print(xbif_file + " has been replicated "+ nclones + " times and saved as " + output_file)