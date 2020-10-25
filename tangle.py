import Crypto.Random
import time
from collections import OrderedDict
from Crypto.Hash import SHA256
from Crypto.Signature import PKCS1_v1_5
from Crypto.PublicKey import RSA
import binascii
from uuid import uuid4
import hashlib
import numpy as np
import random
import matplotlib.pyplot as plt
import pandas as pd
import networkx as nx
from networkx.drawing.nx_agraph import write_dot, graphviz_layout


DIFFICULTY = 3
WALLET_LIST=[]

def generate_wallet():
   
    random_gen = Crypto.Random.new().read
    private_key = RSA.generate(2048, random_gen)
    public_key = private_key.publickey()
    response = {
        'private_key': binascii.hexlify(private_key.export_key(format('PEM'))).decode('ascii'),
        'public_key': binascii.hexlify(public_key.export_key(format('PEM'))).decode('ascii')
    }
    WALLET_LIST.append((public_key, private_key))
    return response


GENESIS_ID = str(uuid4()).replace('-','')
GENESIS_KEYS = generate_wallet()


def hash_data(data):

    data = str(data).encode('utf-8')
    h = hashlib.new('sha256')
    h.update(data)
    return h.hexdigest()


def sign(sender_private_key, transaction_dict):
    private_key_obj = RSA.importKey(binascii.unhexlify(sender_private_key))
    signer_obj = PKCS1_v1_5.new(private_key_obj)
    hash_obj = SHA256.new(str(transaction_dict).encode('utf-8'))
    return binascii.hexlify(signer_obj.sign(hash_obj)).decode('ascii')


def get_previous_hashes(tips):
    previous_hashes = [tangle.transactions[tips[0]].get_hash(),
                            tangle.transactions[tips[0]].get_hash()]

    return previous_hashes


def valid_proof(previous_hashes, transaction_dict, nonce):
    guess = (str(previous_hashes)+ str(transaction_dict) + str(nonce)).encode('utf-8')
    h = hashlib.new('sha256')
    h.update(guess)
    guess_hash = h.hexdigest()
    return guess_hash[:DIFFICULTY] == '0'*DIFFICULTY


def proof_of_work(previous_hashes, transaction_dict):
    nonce = 0
    while not valid_proof(previous_hashes, transaction_dict, nonce):
        nonce = nonce + 1

    return nonce

def generate_transactions(lam=1, size=5, initial=False, initial_count=5, tip_selection_algo='weighted_random_walk'):
    if initial:
        for i in range(initial_count):
            keys = generate_wallet()
            transaction_dict = OrderedDict({
                'sender_public_key': GENESIS_KEYS['public_key'],
                'recipient_public_key': keys['public_key'],
                'amount': 1000
                })
            
            tips = [GENESIS_ID, GENESIS_ID]
            previous_hashes = get_previous_hashes(tips)
            transaction = transaction_block(
                sender_public_key = transaction_dict['sender_public_key'],
                recipient_public_key = transaction_dict['recipient_public_key'],
                amount = transaction_dict['amount'],
                signature = sign(GENESIS_KEYS['private_key'], transaction_dict),
                tx_id = str(uuid4()).replace('-',''),
                approved_tx=tips,
                nonce=proof_of_work(previous_hashes, transaction_dict),
                previous_hashes=previous_hashes)

            tangle.add_transaction(transaction)


    else:
        time_length, _, _ = plt.hist(np.random.poisson(lam, size))
        plt.show()
        
        for t in time_length:
            for i in range(int(t)):
                keys = generate_wallet()
                transaction_dict = OrderedDict({
                'sender_public_key': GENESIS_KEYS['public_key'],
                'recipient_public_key': keys['public_key'],
                'amount': np.random.randint(low=50,high=100)
                })

                tips = tangle.find_tips(algo=tip_selection_algo)
                previous_hashes = get_previous_hashes(tips)
                transaction = transaction_block(
                    sender_public_key = transaction_dict['sender_public_key'],
                    recipient_public_key = transaction_dict['recipient_public_key'],
                    amount = transaction_dict['amount'],
                    signature = sign(GENESIS_KEYS['private_key'], transaction_dict),
                    tx_id = str(uuid4()).replace('-',''),
                    approved_tx=tips,
                    nonce=proof_of_work(previous_hashes, transaction_dict),
                    previous_hashes=previous_hashes)

                tangle.add_transaction(transaction)


def plot_graph():

    edge_list = tangle.edges
    frm = []
    to = []

    for i in edge_list.keys():
        for j in edge_list[i]:
            frm.append(i)
            to.append(j)

    df = pd.DataFrame({ 'from':frm, 'to':to})

    # Build the tangle graph
    G=nx.from_pandas_edgelist(df, 'from', 'to', create_using=nx.DiGraph)

    j=65
    mapping = {}
    cols = []
    size = []

    for i in G:
        wt = tangle.transactions[i].cumulative_weight
        mapping[i]=(chr(j), wt)
        j=j+1
        size.append(1000+500*G.in_degree[i])
        if G.in_degree[i] > 0:
            cols.append('skyblue')   
        else:
            cols.append('lightgreen')
        
    nx.draw(G, labels = mapping, node_color = cols, node_size=size, pos=nx.fruchterman_reingold_layout(G))
    plt.title("Tangle")
    plt.show()


class transaction_block:

    def __init__(self, sender_public_key, recipient_public_key, amount, signature, tx_id, approved_tx, nonce, previous_hashes):
        self.tx_id = tx_id
        self.own_weight = 1
        self.cumulative_weight = 0
        self.nonce = nonce
        self.previous_hashes = previous_hashes
        self.approved_tx = approved_tx,
        self.payload = ''
        self.timestamp = time.time(),
        self.signature = signature
        self.recipient_public_key = recipient_public_key
        self.sender_public_key = sender_public_key
        self.amount = amount

    def get_hash(self):
        transaction_dict = OrderedDict({
            'sender_public_key': self.sender_public_key,
            'recipient_public_key': self.recipient_public_key,
            'amount': self.amount
        })
        return hash_data(transaction_dict)

    def show(self):
        print("Transaction ID = ", self.tx_id)
        print("Cumulative Weight = ", self.cumulative_weight)
        print("Time Stamp = ", time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(self.timestamp[0])))
        print("Recipient Address = ", self.recipient_public_key)
        print("Sender Address = ", self.sender_public_key)
        print("Amount = ", self.amount)
        print('\n')



class Tangle:
    
    def __init__(self):

        self.transactions = {GENESIS_ID:transaction_block(GENESIS_KEYS['public_key'], GENESIS_KEYS['public_key'], 100000,
                                sign(GENESIS_KEYS['private_key'], transaction_dict = OrderedDict({'sender_public_key': GENESIS_KEYS['public_key'],
                                                                                                   'recipient_public_key': GENESIS_KEYS['public_key'],
                                                                                                   'amount': 100000,
                                                                                                    })),
                                GENESIS_ID, None, None, None)}

        self.edges = {GENESIS_ID:[]}
        self.reverse_edges = {GENESIS_ID:[]}

    
    def add_transaction(self, transaction: transaction_block):
        self.transactions[transaction.tx_id] = transaction
        self.add_edges(transaction)
        self.update_cumulative_weights(transaction)


    def add_edges(self, transaction: transaction_block):
        #Creating the forward (in time) edge dict
        approved = transaction.approved_tx[0]
        self.reverse_edges[transaction.tx_id] = []
        if transaction.tx_id not in self.edges:
            self.edges[transaction.tx_id] = approved

        if approved[0] not in self.reverse_edges:
            self.reverse_edges[approved[0]] = [transaction.tx_id]
        else:
            self.reverse_edges[approved[0]].append(transaction.tx_id)

        if approved[1] not in self.reverse_edges:
            self.reverse_edges[approved[1]] = [transaction.tx_id]
        else:
            self.reverse_edges[approved[1]].append(transaction.tx_id)       


    def random_walk_weighted(self, current_node=GENESIS_ID):
        if len(self.reverse_edges[current_node]) == 0:
            return current_node

        elif len(self.reverse_edges[current_node]) < 3:
                option = np.random.choice(np.arange(0,2))
                if option==0:
                    return current_node

        prob = []
        for next_node in self.reverse_edges[current_node]:
            prob.append(self.transactions[next_node].cumulative_weight)

        prob = prob/np.sum(prob)

        choice = np.random.choice(np.arange(0,len(self.reverse_edges[current_node])), p=prob)
        return self.random_walk_weighted(self.reverse_edges[current_node][choice])

    
    def random_walk_unweighted(self, current_node=GENESIS_ID):
        if len(self.reverse_edges[current_node]) == 0:
            return current_node

        elif len(self.reverse_edges[current_node]) < 3:
                option = np.random.choice(np.arange(0,2))
                if option==0:
                    return current_node

        choice = np.random.choice(np.arange(0,len(self.reverse_edges[current_node])))
        return self.random_walk_weighted(self.reverse_edges[current_node][choice])
            

    def find_tips(self, algo='weighted_random_walk'):

        if algo=='recently_added':
            return list(random.sample(set(list(self.transactions.keys())[-2:]), 2))

        elif algo=='unweighted_random_walk':
            tips_list = []
            for n in range(2):
                tips_dict = {}
                for i in range(100):
                    tip = self.random_walk_unweighted()
                    if tip not in tips_dict:
                        tips_dict[tip] = 1
                    else:
                        tips_dict[tip] += 1

                max=0
                max_tip=''
                for i in tips_dict:
                    if tips_dict[i]>max:
                        max = tips_dict[i]
                        max_tip = i
                
                tips_list.append(max_tip)
            
            return tips_list


        elif algo=='weighted_random_walk':
            tips_list = []
            for n in range(2):
                tips_dict = {}
                for i in range(1):
                    tip = self.random_walk_weighted()
                    if tip not in tips_dict:
                        tips_dict[tip] = 1
                    else:
                        tips_dict[tip] += 1

                max=0
                max_tip=''
                for i in tips_dict:
                    if tips_dict[i]>max:
                        max = tips_dict[i]
                        max_tip = i
                
                tips_list.append(max_tip)
            
            return tips_list

        else:
            return list(random.sample(set(self.transactions.keys()), 2))
        




    def update_cumulative_weights(self, current_node):
        if current_node.tx_id is GENESIS_ID:
            current_node.cumulative_weight += 1

        else:
            current_node.cumulative_weight += 1
            a, b = current_node.approved_tx[0]
            self.update_cumulative_weights(self.transactions[a])
            self.update_cumulative_weights(self.transactions[b])


    
if __name__=="__main__":
    tangle = Tangle()
    generate_transactions(initial= True, size=5)
    generate_transactions(size=20, tip_selection_algo='weighted_random_walk')
    # for i in tangle.transactions:
    #     tangle.transactions[i].show()

    plot_graph()
    