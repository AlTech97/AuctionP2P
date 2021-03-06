package pack;

import java.util.ArrayList;
import java.util.Date;

public interface AuctionMechanismInterface {

    /**
     * Creates a new auction for a good.
     * @param _auction_name a String, the name identify the auction.
     * @param _end_time a Date that is the end time of an auction.
     * @param _reserved_price a double value that is the reserve minimum pricing selling.
     * @param _description a String describing the selling goods in the auction.
     * @return true if the auction is correctly created, false otherwise.
     */
    public boolean createAuction(String _auction_name, Date _end_time, double _reserved_price, String _description);

    /**
     * Remove an auction. Only the owner will have success
     * @param _auction_name a String, the name of the auction.
     * @return true if the auction is correctly removed, false otherwise.
     */
    public boolean removeAuction(String _auction_name);

    /**
     * Update an auction by replacing the old one with the new one.
     * Note that only the owner can update the auction
     * @param _auction an Auction that replacing the current one
     * @return true if the update is successful, false otherwise
     */
    public boolean updateAuction(Auction _auction);

    /**
     * follow an auction to receive all its updates
     * @param _auction_name a String, the name of the auction
     * @return true if the the operation is successful, false otherwise
     */
    public boolean followAuction(String _auction_name);

    /**
     * stop following an auction to no longer receive updates
     * @param _auction_name a String, the name of the auction
     * @return true if the the operation is successful, false otherwise
     */
    public boolean unfollowAuction(String _auction_name);

    /**
     * Places a bid for an auction if it is not already ended.
     * @param _auction_name a String, the name of the auction.
     * @param _bid_amount a double value, the bid for an auction.
     * @return a String value that is the status of the auction.
     */
    public String placeAbid(String _auction_name, double _bid_amount);

    /**
     * Checks the status of the auction.
     * @param _auction_name a String, the name of the auction.
     * @return a String value that is the status of the auction. Null if the auction doesn't exist
     */
    public String checkAuction(String _auction_name);

    /**
     * Search for an auction among those created by the peer
     * @param _auction_name a String, the name of the auction
     * @return the Auction object found, null otherwise
     */
    public Auction localSearch (String _auction_name);

    /**
     * Search for an auction among all existing ones
     * @param _auction_name a String, the name of the auction
     * @return the Auction object found, null otherwise
     */
    public Auction globalSearch(String _auction_name);

    /**
     * get the unique name of all existing auctions
     * @return an ArrayList that contains the list of the names
     */
    public ArrayList<String> getEveryAuctionNames();

    /**
     *  Get the list of all open auctions
     * @return an ArrayList that contains all open auctions
     */
    public ArrayList<Auction> getOpenAuctions();

    /**
     * Leave the P2P network and unsubscribe from the auctions
     * @return true if unsubscribe from all the auctions followed is done correctly, false otherwise
     */
    public boolean leaveNetwork();

}
