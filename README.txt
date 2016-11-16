Datacommunication Project 2, phase 1

Goal:
1) Edit the data model to get a decent rate on the server

Goal --> Done
How:
Use ConcurrentHashMap and ConcurrentSkipListSet
1 static array of ConcurrentHashMap<String, ConcurrentSkipListSet> --> for getting parcel trail
1 static array of ConcurrentHashMap<String, Long> --> for getting station stop count

static array has 36 slots
items are put in slot according to their first character

I have a ConcurrentHashMap<String, Integer> to map the character to their slot integet for the static array

