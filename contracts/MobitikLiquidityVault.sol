// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/**
 * @title MobitikLiquidityVault
 * @notice Authorized BNB Smart Chain (BEP-20) Liquidity Pool & USDT Settlement Vault for MOBITIK (MBTK).
 * @dev Enforces the configured reference exchange rate (default 1 MBTK = 0.15 USDT = 1500 bps),
 *      requires actual funded USDT liquidity before executing swaps or withdrawals,
 *      protects against replay attacks via idempotency keys, and provides emergency pause controls.
 */
interface IBEP20 {
    function totalSupply() external view returns (uint256);
    function balanceOf(address account) external view returns (uint256);
    function transfer(address recipient, uint256 amount) external returns (bool);
    function allowance(address owner, address spender) external view returns (uint256);
    function approve(address spender, uint256 amount) external returns (bool);
    function transferFrom(address sender, address recipient, uint256 amount) external returns (bool);
}

contract MobitikLiquidityVault {
    IBEP20 public immutable mbtkToken;
    IBEP20 public immutable usdtToken;

    address public owner;
    address public authorizedRelayer;

    // Reference rate in 1e4 precision: 1500 = 0.1500 USDT per 1 MBTK
    uint256 public referenceRateBps = 1500;
    // Swap fee in basis points: 100 = 1.00%
    uint256 public swapFeeBps = 100;
    // Minimum and maximum single USDT withdrawal (18 decimals on BSC)
    uint256 public minWithdrawalUsdt = 5 * 1e18;
    uint256 public maxWithdrawalUsdt = 5000 * 1e18;

    bool public swapsPaused = false;
    bool public withdrawalsPaused = false;

    mapping(bytes32 => bool) public processedIdempotencyKeys;

    event SwapExecuted(
        address indexed user,
        uint256 mbtkIn,
        uint256 grossUsdt,
        uint256 feeUsdt,
        uint256 netUsdtOut
    );
    event WithdrawalSettled(
        address indexed destination,
        uint256 netUsdtAmount,
        bytes32 indexed idempotencyKey
    );
    event ParametersUpdated(
        uint256 referenceRateBps,
        uint256 swapFeeBps,
        bool swapsPaused,
        bool withdrawalsPaused
    );

    modifier onlyOwner() {
        require(msg.sender == owner, "MobitikVault: caller is not owner");
        _;
    }

    modifier onlyRelayerOrOwner() {
        require(msg.sender == owner || msg.sender == authorizedRelayer, "MobitikVault: unauthorized");
        _;
    }

    constructor(address _mbtkToken, address _usdtToken, address _relayer) {
        require(_mbtkToken != address(0) && _usdtToken != address(0), "MobitikVault: zero token address");
        owner = msg.sender;
        mbtkToken = IBEP20(_mbtkToken);
        usdtToken = IBEP20(_usdtToken);
        authorizedRelayer = _relayer;
    }

    /**
     * @notice Swaps BEP-20 MBTK for BEP-20 USDT at the configured reference price (0.15 USDT/MBTK)
     *         only when the vault holds sufficient funded USDT liquidity.
     */
    function swapMbtkForUsdt(uint256 mbtkAmount) external {
        require(!swapsPaused, "Swaps temporarily unavailable");
        require(mbtkAmount > 0, "Amount must be > 0");

        uint256 grossUsdt = (mbtkAmount * referenceRateBps) / 10000;
        uint256 feeUsdt = (grossUsdt * swapFeeBps) / 10000;
        uint256 netUsdt = grossUsdt - feeUsdt;
        require(netUsdt > 0, "Net USDT output must be > 0");

        uint256 availableLiquidity = usdtToken.balanceOf(address(this));
        require(availableLiquidity >= netUsdt, "Swaps temporarily unavailable: insufficient USDT liquidity");

        require(mbtkToken.transferFrom(msg.sender, address(this), mbtkAmount), "MBTK transfer failed");
        require(usdtToken.transfer(msg.sender, netUsdt), "USDT payout failed");

        emit SwapExecuted(msg.sender, mbtkAmount, grossUsdt, feeUsdt, netUsdt);
    }

    /**
     * @notice Settles an authorized external USDT withdrawal with replay protection.
     */
    function settleAuthorizedWithdrawal(
        address destination,
        uint256 netUsdtAmount,
        bytes32 idempotencyKey
    ) external onlyRelayerOrOwner {
        require(!withdrawalsPaused, "Withdrawals paused");
        require(destination != address(0), "Invalid destination");
        require(!processedIdempotencyKeys[idempotencyKey], "Duplicate idempotency key");
        require(netUsdtAmount >= minWithdrawalUsdt && netUsdtAmount <= maxWithdrawalUsdt, "Outside withdrawal limits");
        require(usdtToken.balanceOf(address(this)) >= netUsdtAmount, "Insufficient vault USDT liquidity");

        processedIdempotencyKeys[idempotencyKey] = true;
        require(usdtToken.transfer(destination, netUsdtAmount), "USDT transfer failed");

        emit WithdrawalSettled(destination, netUsdtAmount, idempotencyKey);
    }

    function updateParameters(
        uint256 _referenceRateBps,
        uint256 _swapFeeBps,
        bool _swapsPaused,
        bool _withdrawalsPaused
    ) external onlyOwner {
        require(_referenceRateBps > 0 && _swapFeeBps <= 2000, "Invalid rate or fee");
        referenceRateBps = _referenceRateBps;
        swapFeeBps = _swapFeeBps;
        swapsPaused = _swapsPaused;
        withdrawalsPaused = _withdrawalsPaused;
        emit ParametersUpdated(_referenceRateBps, _swapFeeBps, _swapsPaused, _withdrawalsPaused);
    }
}
